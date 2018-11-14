/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.impl.light.LightReferenceListBuilder
import com.intellij.psi.impl.light.LightTypeParameterBuilder
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.elements.KotlinLightTypeParameterListBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import java.text.StringCharacterIterator

internal fun buildTypeParameterList(
    declaration: KtTypeParameterListOwner,
    owner: PsiTypeParameterListOwner,
    support: UltraLightSupport
): PsiTypeParameterList {
    val tpList = KotlinLightTypeParameterListBuilder(owner)
    for ((i, ktParam) in declaration.typeParameters.withIndex()) {
        tpList.addParameter(object : LightTypeParameterBuilder(ktParam.name.orEmpty(), owner, i) {
            private val superList: LightReferenceListBuilder by lazyPub {
                val boundList =
                    KotlinLightReferenceListBuilder(manager, PsiReferenceList.Role.EXTENDS_BOUNDS_LIST)
                if (ktParam.extendsBound != null || declaration.typeConstraints.isNotEmpty()) {
                    val boundTypes = (ktParam.resolve() as? TypeParameterDescriptor)?.upperBounds.orEmpty()
                        .mapNotNull { it.asPsiType(ktParam, support, TypeMappingMode.DEFAULT, this) as? PsiClassType }
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

// copy-pasted from kotlinInternalUastUtils.kt and post-processed
internal fun KotlinType.asPsiType(
    declaration: KtDeclaration,
    support: UltraLightSupport,
    mode: TypeMappingMode,
    psiContext: PsiElement
): PsiType {
    val typeFqName = constructor.declarationDescriptor?.fqNameSafe?.asString()
    if (typeFqName == "kotlin.Unit" && declaration is KtFunction) return PsiType.VOID

    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
    typeMapper(support).mapType(this, signatureWriter, mode)
    val signature = StringCharacterIterator(signatureWriter.toString())

    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return PsiType.NULL

    val type = ClsTypeElementImpl(psiContext, typeText, '\u0000').type
    if (type is PsiArrayType && psiContext is KtUltraLightParameter && psiContext.isVarArgs) {
        return PsiEllipsisType(type.componentType, type.annotationProvider)
    }
    return type
}

internal fun typeMapper(support: UltraLightSupport): KotlinTypeMapper = KotlinTypeMapper(
    BindingContext.EMPTY, ClassBuilderMode.LIGHT_CLASSES,
    IncompatibleClassTracker.DoNothing, support.moduleName,
    JvmTarget.JVM_1_8,
    true, false
)
