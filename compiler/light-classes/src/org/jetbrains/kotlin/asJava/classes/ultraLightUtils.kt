/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.cache.ModifierFlags
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.impl.light.*
import com.intellij.util.BitUtil.isSet
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.KotlinLightTypeParameterListBuilder
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.org.objectweb.asm.Opcodes
import java.text.StringCharacterIterator

internal fun buildTypeParameterList(
    declaration: CallableMemberDescriptor,
    owner: PsiTypeParameterListOwner,
    support: UltraLightSupport
): PsiTypeParameterList = buildTypeParameterList(
    declaration, owner, support,
    object : TypeParametersSupport<CallableMemberDescriptor, TypeParameterDescriptor> {
        override fun parameters(declaration: CallableMemberDescriptor) = declaration.typeParameters

        override fun name(typeParameter: TypeParameterDescriptor) = typeParameter.name.asString()

        override fun hasNonTrivialBounds(
            declaration: CallableMemberDescriptor,
            typeParameter: TypeParameterDescriptor
        ) = typeParameter.upperBounds.any { !KotlinBuiltIns.isDefaultBound(it) }

        override fun asDescriptor(typeParameter: TypeParameterDescriptor) = typeParameter
    }
)

internal fun buildTypeParameterList(
    declaration: KtTypeParameterListOwner,
    owner: PsiTypeParameterListOwner,
    support: UltraLightSupport
): PsiTypeParameterList = buildTypeParameterList(
    declaration, owner, support,
    object : TypeParametersSupport<KtTypeParameterListOwner, KtTypeParameter> {
        override fun parameters(declaration: KtTypeParameterListOwner) = declaration.typeParameters
        override fun name(typeParameter: KtTypeParameter) = typeParameter.name

        override fun hasNonTrivialBounds(
            declaration: KtTypeParameterListOwner,
            typeParameter: KtTypeParameter
        ) = typeParameter.extendsBound != null || declaration.typeConstraints.isNotEmpty()

        override fun asDescriptor(typeParameter: KtTypeParameter) = typeParameter.resolve() as? TypeParameterDescriptor
    }
)

interface TypeParametersSupport<D, T> {
    fun parameters(declaration: D): List<T>
    fun name(typeParameter: T): String?
    fun hasNonTrivialBounds(declaration: D, typeParameter: T): Boolean
    fun asDescriptor(typeParameter: T): TypeParameterDescriptor?
}

internal fun <D, T> buildTypeParameterList(
    declaration: D,
    owner: PsiTypeParameterListOwner,
    support: UltraLightSupport,
    typeParametersSupport: TypeParametersSupport<D, T>
): PsiTypeParameterList {
    val tpList = KotlinLightTypeParameterListBuilder(owner)
    for ((i, param) in typeParametersSupport.parameters(declaration).withIndex()) {
        tpList.addParameter(object : LightTypeParameterBuilder(typeParametersSupport.name(param).orEmpty(), owner, i) {
            private val superList: LightReferenceListBuilder by lazyPub {
                val boundList =
                    KotlinLightReferenceListBuilder(manager, PsiReferenceList.Role.EXTENDS_BOUNDS_LIST)

                if (typeParametersSupport.hasNonTrivialBounds(declaration, param)) {
                    val boundTypes = typeParametersSupport.asDescriptor(param)?.upperBounds.orEmpty()
                        .mapNotNull { it.asPsiType(support, TypeMappingMode.DEFAULT, this) as? PsiClassType }
                    val hasDefaultBound = boundTypes.size == 1 && boundTypes[0].equalsToText(CommonClassNames.JAVA_LANG_OBJECT)
                    if (!hasDefaultBound) {
                        boundTypes.forEach(boundList::addReference)
                    }
                }
                boundList
            }

            override fun getExtendsList(): LightReferenceListBuilder = superList

            override fun getParent(): PsiElement = tpList
            override fun getContainingFile(): PsiFile = owner.containingFile
        })
    }
    return tpList
}

internal fun KtDeclaration.getKotlinType(): KotlinType? {
    val descriptor = resolve()
    return when (descriptor) {
        is ValueDescriptor -> descriptor.type
        is CallableDescriptor -> descriptor.returnType
        else -> null
    }
}

internal fun KtDeclaration.resolve() = LightClassGenerationSupport.getInstance(project).resolveToDescriptor(this)
internal fun KtElement.analyze() = LightClassGenerationSupport.getInstance(project).analyze(this)

// copy-pasted from kotlinInternalUastUtils.kt and post-processed
internal fun KotlinType.asPsiType(
    support: UltraLightSupport,
    mode: TypeMappingMode,
    psiContext: PsiElement
): PsiType = support.mapType(psiContext) { typeMapper, signatureWriter ->
    typeMapper.mapType(this, signatureWriter, mode)
}

internal fun UltraLightSupport.mapType(
    psiContext: PsiElement,
    mapTypeToSignatureWriter: (KotlinTypeMapper, JvmSignatureWriter) -> Unit
): PsiType {
    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.SKIP_CHECKS)
    mapTypeToSignatureWriter(typeMapper, signatureWriter)
    val canonicalSignature = signatureWriter.toString()
    return createTypeFromCanonicalText(canonicalSignature, psiContext)
}

fun createTypeFromCanonicalText(
    canonicalSignature: String,
    psiContext: PsiElement
): PsiType {
    val signature = StringCharacterIterator(canonicalSignature)

    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return PsiType.NULL

    val type = ClsTypeElementImpl(psiContext, typeText, '\u0000').type
    if (type is PsiArrayType && psiContext is KtUltraLightParameter && psiContext.isVarArgs) {
        return PsiEllipsisType(type.componentType, type.annotationProvider)
    }
    return type
}

// Returns null when type is unchanged
fun KotlinType.cleanFromAnonymousTypes(): KotlinType? {
    val returnTypeClass = constructor.declarationDescriptor as? ClassDescriptor ?: return null
    if (DescriptorUtils.isAnonymousObject(returnTypeClass)) {
        // We choose just the first supertype because:
        // - In public declarations, object literals should always have a single supertype (otherwise it's an error)
        // - For private declarations, they might have more than one supertype
        //   but it looks like it's not important how we choose a representative for them
        val representative = returnTypeClass.defaultType.supertypes().firstOrNull() ?: return null
        return representative.cleanFromAnonymousTypes() ?: representative
    }

    if (arguments.isEmpty()) return null

    var wasUpdates = false

    val newArguments = arguments.map { typeProjection ->
        val updatedType =
            typeProjection.takeUnless { it.isStarProjection }
                ?.type?.cleanFromAnonymousTypes()
                ?: return@map typeProjection

        wasUpdates = true

        TypeProjectionImpl(typeProjection.projectionKind, updatedType)
    }

    if (!wasUpdates) return null

    return replace(newArguments = newArguments)
}

fun KtUltraLightClass.createGeneratedMethodFromDescriptor(
    descriptor: FunctionDescriptor,
    declarationForOrigin: KtDeclaration? = null
): KtLightMethod {
    val lightMethod = lightMethod(descriptor)
    val kotlinOrigin =
        declarationForOrigin
            ?: DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtDeclaration
            ?: kotlinOrigin

    val wrapper = KtUltraLightMethodForDescriptor(descriptor, lightMethod, kotlinOrigin, support, this)

    descriptor.extensionReceiverParameter?.let { receiver ->
        lightMethod.addParameter(KtUltraLightParameterForDescriptor(receiver, kotlinOrigin, support, wrapper))
    }

    for (valueParameter in descriptor.valueParameters) {
        lightMethod.addParameter(KtUltraLightParameterForDescriptor(valueParameter, kotlinOrigin, support, wrapper))
    }

    lightMethod.setMethodReturnType {
        support.mapType(wrapper) { typeMapper, signatureWriter ->
            typeMapper.mapReturnType(descriptor, signatureWriter)
        }
    }

    return wrapper
}

private fun KtUltraLightClass.lightMethod(
    descriptor: FunctionDescriptor
): LightMethodBuilder {
    val name = support.typeMapper.mapFunctionName(descriptor, OwnerKind.IMPLEMENTATION)

    val accessFlags: Int by lazyPub {
        val asmFlags = AsmUtil.getMethodAsmFlags(descriptor, OwnerKind.IMPLEMENTATION, support.deprecationResolver)
        packMethodFlags(asmFlags, JvmCodegenUtil.isJvmInterface(kotlinOrigin.resolve() as? ClassDescriptor))
    }

    return LightMethodBuilder(
        manager, language, name,
        LightParameterListBuilder(manager, language),
        object : LightModifierList(manager, language) {
            override fun hasModifierProperty(name: String) = ModifierFlags.hasModifierProperty(name, accessFlags)
        }
    )
}

private fun packCommonFlags(access: Int): Int {
    var flags = when {
        isSet(access, Opcodes.ACC_PRIVATE) -> ModifierFlags.PRIVATE_MASK
        isSet(access, Opcodes.ACC_PROTECTED) -> ModifierFlags.PROTECTED_MASK
        isSet(access, Opcodes.ACC_PUBLIC) -> ModifierFlags.PUBLIC_MASK
        else -> ModifierFlags.PACKAGE_LOCAL_MASK
    }

    if (isSet(access, Opcodes.ACC_STATIC)) {
        flags = flags or ModifierFlags.STATIC_MASK
    }

    if (isSet(access, Opcodes.ACC_FINAL)) {
        flags = flags or ModifierFlags.FINAL_MASK
    }

    return flags
}

private fun packMethodFlags(access: Int, isInterface: Boolean): Int {
    var flags = packCommonFlags(access)

    if (isSet(access, Opcodes.ACC_SYNCHRONIZED)) {
        flags = flags or ModifierFlags.SYNCHRONIZED_MASK
    }

    if (isSet(access, Opcodes.ACC_NATIVE)) {
        flags = flags or ModifierFlags.NATIVE_MASK
    }

    if (isSet(access, Opcodes.ACC_STRICT)) {
        flags = flags or ModifierFlags.STRICTFP_MASK
    }

    if (isSet(access, Opcodes.ACC_ABSTRACT)) {
        flags = flags or ModifierFlags.ABSTRACT_MASK
    } else if (isInterface && !isSet(access, Opcodes.ACC_STATIC)) {
        flags = flags or ModifierFlags.DEFAULT_MASK
    }

    return flags
}
