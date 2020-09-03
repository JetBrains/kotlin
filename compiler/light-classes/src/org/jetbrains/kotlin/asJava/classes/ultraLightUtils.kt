/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.google.common.collect.Lists
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.psi.impl.cache.ModifierFlags
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.impl.light.*
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.BitUtil.isSet
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.UltraLightClassModifierExtension
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.KotlinLightTypeParameterListBuilder
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotationForSourceEntry
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.psiType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.org.objectweb.asm.Opcodes
import java.text.StringCharacterIterator

private interface TypeParametersSupport<D, T> {
    fun parameters(declaration: D): List<T>
    fun name(typeParameter: T): String?
    fun hasNonTrivialBounds(declaration: D, typeParameter: T): Boolean
    fun asDescriptor(typeParameter: T): TypeParameterDescriptor?
}

private val supportForDescriptor = object : TypeParametersSupport<CallableMemberDescriptor, TypeParameterDescriptor> {

    override fun parameters(declaration: CallableMemberDescriptor) = declaration.typeParameters

    override fun name(typeParameter: TypeParameterDescriptor) = typeParameter.name.asString()

    override fun hasNonTrivialBounds(
        declaration: CallableMemberDescriptor,
        typeParameter: TypeParameterDescriptor,
    ) = typeParameter.upperBounds.any { !KotlinBuiltIns.isDefaultBound(it) }

    override fun asDescriptor(typeParameter: TypeParameterDescriptor) = typeParameter
}

private val supportForSourceDeclaration = object : TypeParametersSupport<KtTypeParameterListOwner, KtTypeParameter> {

    override fun parameters(declaration: KtTypeParameterListOwner) = declaration.typeParameters

    override fun name(typeParameter: KtTypeParameter) = typeParameter.name

    override fun hasNonTrivialBounds(
        declaration: KtTypeParameterListOwner,
        typeParameter: KtTypeParameter,
    ) = typeParameter.extendsBound != null || declaration.typeConstraints.isNotEmpty()

    override fun asDescriptor(typeParameter: KtTypeParameter) = typeParameter.resolve() as? TypeParameterDescriptor
}

internal fun buildTypeParameterListForDescriptor(
    declaration: CallableMemberDescriptor,
    owner: PsiTypeParameterListOwner,
    support: KtUltraLightSupport,
): PsiTypeParameterList = buildTypeParameterList(declaration, owner, support, supportForDescriptor)


internal fun buildTypeParameterListForSourceDeclaration(
    declaration: KtTypeParameterListOwner,
    owner: PsiTypeParameterListOwner,
    support: KtUltraLightSupport,
): PsiTypeParameterList = buildTypeParameterList(declaration, owner, support, supportForSourceDeclaration)

private fun <D, T> buildTypeParameterList(
    declaration: D,
    owner: PsiTypeParameterListOwner,
    support: KtUltraLightSupport,
    typeParametersSupport: TypeParametersSupport<D, T>,
): PsiTypeParameterList {

    val tpList = KotlinLightTypeParameterListBuilder(owner)

    for ((i, param) in typeParametersSupport.parameters(declaration).withIndex()) {

        val referenceListBuilder = { element: PsiElement ->
            val boundList = KotlinLightReferenceListBuilder(element.manager, PsiReferenceList.Role.EXTENDS_BOUNDS_LIST)

            if (typeParametersSupport.hasNonTrivialBounds(declaration, param)) {
                val boundTypes = typeParametersSupport.asDescriptor(param)
                    ?.upperBounds
                    .orEmpty()
                    .mapNotNull { it.asPsiType(support, TypeMappingMode.DEFAULT, element) as? PsiClassType }

                val hasDefaultBound = boundTypes.size == 1 && boundTypes[0].equalsToText(CommonClassNames.JAVA_LANG_OBJECT)
                if (!hasDefaultBound) boundTypes.forEach(boundList::addReference)
            }
            boundList
        }

        val parameterName = typeParametersSupport.name(param).orEmpty()

        tpList.addParameter(KtUltraLightTypeParameter(parameterName, owner, tpList, i, referenceListBuilder))
    }

    return tpList
}


internal fun KtDeclaration.getKotlinType(): KotlinType? {
    val descriptor = resolve()
    return when (descriptor) {
        is ValueDescriptor -> descriptor.type
        is CallableDescriptor -> if (descriptor is FunctionDescriptor && descriptor.isSuspend)
            descriptor.module.builtIns.nullableAnyType else descriptor.returnType
        else -> null
    }
}

internal fun KtDeclaration.resolve() = LightClassGenerationSupport.getInstance(project).resolveToDescriptor(this)
internal fun KtElement.analyze() = LightClassGenerationSupport.getInstance(project).analyze(this)
internal fun KtAnnotationEntry.analyzeAnnotation() = LightClassGenerationSupport.getInstance(project).analyzeAnnotation(this)

// copy-pasted from kotlinInternalUastUtils.kt and post-processed
internal fun KotlinType.asPsiType(
    support: KtUltraLightSupport,
    mode: TypeMappingMode,
    psiContext: PsiElement,
): PsiType = support.mapType(psiContext) { typeMapper, signatureWriter ->
    typeMapper.mapType(this, signatureWriter, mode)
}

internal fun KtUltraLightSupport.mapType(
    psiContext: PsiElement,
    mapTypeToSignatureWriter: (KotlinTypeMapper, JvmSignatureWriter) -> Unit,
): PsiType {
    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.SKIP_CHECKS)
    mapTypeToSignatureWriter(typeMapper, signatureWriter)
    val canonicalSignature = signatureWriter.toString()
    return createTypeFromCanonicalText(canonicalSignature, psiContext)
}

fun createTypeFromCanonicalText(
    canonicalSignature: String,
    psiContext: PsiElement,
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

fun tryGetPredefinedName(klass: ClassDescriptor): String? {
    val sourceClass = (klass.source as? KotlinSourceElement)?.psi as? KtClassOrObject

    return if (sourceClass?.safeIsLocal() == true)
        (sourceClass.nameAsName ?: SpecialNames.NO_NAME_PROVIDED).asString()
    else null
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
    declarationOriginKindForOrigin: JvmDeclarationOriginKind = JvmDeclarationOriginKind.OTHER,
    declarationForOrigin: KtDeclaration? = null
): KtLightMethod {

    val kotlinOrigin =
        declarationForOrigin
            ?: DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtDeclaration
            ?: kotlinOrigin

    val lightMemberOrigin = LightMemberOriginForDeclaration(kotlinOrigin, declarationOriginKindForOrigin)

    return KtUltraLightMethodForDescriptor(descriptor, lightMethod(descriptor), lightMemberOrigin, support, this)
}

private fun KtUltraLightClass.lightMethod(
    descriptor: FunctionDescriptor,
): LightMethodBuilder {
    val name = if (descriptor is ConstructorDescriptor) name else support.typeMapper.mapFunctionName(descriptor, OwnerKind.IMPLEMENTATION)

    val accessFlags: Int by lazyPub {
        val asmFlags = AsmUtil.getMethodAsmFlags(
            descriptor,
            OwnerKind.IMPLEMENTATION,
            support.deprecationResolver,
            support.typeMapper.jvmDefaultMode,
        )
        packMethodFlags(asmFlags, JvmCodegenUtil.isJvmInterface(kotlinOrigin.resolve() as? ClassDescriptor))
    }

    return LightMethodBuilder(
        manager, language, name,
        LightParameterListBuilder(manager, language),
        object : LightModifierList(manager, language) {
            override fun hasModifierProperty(name: String) = ModifierFlags.hasModifierProperty(name, accessFlags)
        },
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

internal fun KtModifierListOwner.isHiddenByDeprecation(support: KtUltraLightSupport): Boolean {
    if (annotationEntries.isEmpty()) return false
    val annotations = annotationEntries.filter { annotation ->
        annotation.looksLikeDeprecated()
    }
    if (annotations.isNotEmpty()) { // some candidates found
        val deprecated = support.findAnnotation(this, StandardNames.FqNames.deprecated)?.second
        return (deprecated?.argumentValue("level") as? EnumValue)?.enumEntryName?.asString() == "HIDDEN"
    } else {
        return false
    }
}

fun KtAnnotationEntry.looksLikeDeprecated(): Boolean {
    val arguments = valueArguments.filterIsInstance<KtValueArgument>().filterIndexed { index, valueArgument ->
        index == 2 || valueArgument.looksLikeLevelArgument() // for named/not named arguments
    }
    for (argument in arguments) {
        val hiddenByDotQualifiedCandidates = argument.children.filterIsInstance<KtDotQualifiedExpression>().filter {
            val lastChild = it.children.last()
            if (lastChild is KtNameReferenceExpression)
                lastChild.getReferencedName() == "HIDDEN"
            else
                false
        }
        val hiddenByNameReferenceExpressionCandidates = argument.children.filterIsInstance<KtNameReferenceExpression>().filter {
            it.getReferencedName() == "HIDDEN"
        }
        if (hiddenByDotQualifiedCandidates.isNotEmpty() || hiddenByNameReferenceExpressionCandidates.isNotEmpty())
            return true
    }
    return false
}

fun KtValueArgument.looksLikeLevelArgument(): Boolean {
    return children.filterIsInstance<KtValueArgumentName>().any { it.asName.asString() == "level" }
}

internal fun KtAnnotated.isJvmStatic(support: KtUltraLightSupport): Boolean =
    support.findAnnotation(this, JVM_STATIC_ANNOTATION_FQ_NAME) !== null

internal fun KtDeclaration.simpleVisibility(): String = when {
    hasModifier(KtTokens.PRIVATE_KEYWORD) -> PsiModifier.PRIVATE
    hasModifier(KtTokens.PROTECTED_KEYWORD) -> PsiModifier.PROTECTED
    else -> PsiModifier.PUBLIC
}

internal fun KtModifierListOwner.isDeprecated(support: KtUltraLightSupport? = null): Boolean {
    val modifierList = this.modifierList ?: return false
    if (modifierList.annotationEntries.isEmpty()) return false

    val deprecatedFqName = StandardNames.FqNames.deprecated
    val deprecatedName = deprecatedFqName.shortName().asString()

    for (annotationEntry in modifierList.annotationEntries) {
        // If it's not a user type, it's definitely not a reference to deprecated
        val typeElement = annotationEntry.typeReference?.typeElement as? KtUserType ?: continue

        val fqName = toQualifiedName(typeElement) ?: continue

        if (fqName == deprecatedFqName) return true
        if (fqName.asString() == deprecatedName) return true
    }

    return support?.findAnnotation(this, StandardNames.FqNames.deprecated) !== null
}

private fun toQualifiedName(userType: KtUserType): FqName? {
    val reversedNames = Lists.newArrayList<String>()

    var current: KtUserType? = userType
    while (current != null) {
        val name = current.referencedName ?: return null

        reversedNames.add(name)
        current = current.qualifier
    }

    return FqName.fromSegments(ContainerUtil.reverse(reversedNames))
}

internal fun ConstantValue<*>.createPsiLiteral(parent: PsiElement): PsiExpression? {
    val asString = asStringForPsiLiteral(parent)
    val instance = PsiElementFactory.getInstance(parent.project)
    return try {
        instance.createExpressionFromText(asString, parent)
    } catch (_: IncorrectOperationException) {
        null
    }
}

private fun escapeString(str: String): String = buildString {
    str.forEach { char ->
        val escaped = when (char) {
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            '\"' -> "\\\""
            '\\' -> "\\\\"
            else -> "$char"
        }
        append(escaped)
    }
}

private fun ConstantValue<*>.asStringForPsiLiteral(parent: PsiElement): String =
    when (this) {
        is NullValue -> "null"
        is StringValue -> "\"${escapeString(value)}\""
        is KClassValue -> {
            val value = (value as KClassValue.Value.NormalClass).value
            val arrayPart = "[]".repeat(value.arrayNestedness)
            val fqName = value.classId.asSingleFqName()
            val canonicalText = psiType(
                fqName.asString(), parent, boxPrimitiveType = value.arrayNestedness > 0,
            ).let(TypeConversionUtil::erasure).getCanonicalText(false)

            "$canonicalText$arrayPart.class"
        }
        is EnumValue -> "${enumClassId.asSingleFqName().asString()}.$enumEntryName"
        else -> when (value) {
            is Long -> "${value}L"
            is Float -> "${value}f"
            else -> value.toString()
        }
    }


/***
 * @see org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
 */
fun KotlinType.tryResolveMarkerInterfaceFQName(): String? {

    val classId = constructor.declarationDescriptor.classId

    for (mapping in JavaToKotlinClassMap.mutabilityMappings) {
        if (mapping.kotlinReadOnly == classId) {
            return "kotlin.jvm.internal.markers.KMappedMarker"
        } else if (mapping.kotlinMutable == classId) {
            return "kotlin.jvm.internal.markers.K" + classId.relativeClassName.asString()
                .replace("MutableEntry", "Entry") // kotlin.jvm.internal.markers.KMutableMap.Entry for some reason
                .replace(".", "$")
        }
    }

    return null
}

internal inline fun Project.applyCompilerPlugins(body: (UltraLightClassModifierExtension) -> Unit) {
    UltraLightClassModifierExtension.getInstances(this).forEach { body(it) }
}

internal fun <L : Any> L.invalidAccess(): Nothing =
    error("Cls delegate shouldn't be loaded for not too complex ultra-light classes! Qualified name: ${javaClass.name}")


inline fun <T> runReadAction(crossinline runnable: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
}

@Suppress("NOTHING_TO_INLINE")
inline fun KtClassOrObject.safeIsLocal(): Boolean = runReadAction { this.isLocal }

@Suppress("NOTHING_TO_INLINE")
inline fun KtFile.safeIsScript() = runReadAction { this.isScript() }

@Suppress("NOTHING_TO_INLINE")
inline fun KtFile.safeScript() = runReadAction { this.script }

internal fun KtUltraLightSupport.findAnnotation(owner: KtAnnotated, fqName: FqName): Pair<KtAnnotationEntry, AnnotationDescriptor>? {

    val candidates = owner.annotationEntries
        .filter {
            it.shortName?.let { name ->
                name == fqName.shortName() || possiblyHasAlias(owner.containingKtFile, name)
            } ?: false
        }

    for (entry in candidates) {
        val descriptor = entry.analyzeAnnotation()
        if (descriptor?.fqName == fqName) {
            return Pair(entry, descriptor)
        }
    }

    if (owner is KtPropertyAccessor) {
        // We might have from the beginning just resolve the descriptor of the accessor
        // But we trying to avoid analysis in case property doesn't have any relevant annotations at all
        // (in case of `findAnnotation` returns null)
        if (findAnnotation(owner.property, fqName) == null) return null

        val accessorDescriptor = owner.resolve() ?: return null

        // Just reuse the logic of use-site targeted annotation from the compiler
        val annotationDescriptor = accessorDescriptor.annotations.findAnnotation(fqName) ?: return null
        val entry = annotationDescriptor.source.getPsi() as? KtAnnotationEntry ?: return null

        return entry to annotationDescriptor
    }

    return null
}

internal fun List<KtAnnotationEntry>.toLightAnnotations(
    parent: PsiElement,
    site: AnnotationUseSiteTarget?
): List<KtLightAnnotationForSourceEntry> =
    filter {
        it.useSiteTarget?.getAnnotationUseSiteTarget() == site
    }.map { entry ->
        KtLightAnnotationForSourceEntry(
            name = entry.shortName?.identifier,
            lazyQualifiedName = { entry.analyzeAnnotation()?.fqName?.asString() },
            kotlinOrigin = entry,
            parent = parent,
            lazyClsDelegate = null
        )
    }