/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.meta.PsiMetaData
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightNullabilityAnnotation
import org.jetbrains.kotlin.asJava.elements.psiType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.nullability

class KtUltraLightNullabilityAnnotation(
    member: KtUltraLightElementWithNullabilityAnnotation<*, *>,
    parent: PsiElement
) : KtLightNullabilityAnnotation<KtUltraLightElementWithNullabilityAnnotation<*, *>>(member, parent) {
    override fun getQualifiedName(): String? {
        val kotlinType = member.kotlinTypeForNullabilityAnnotation?.takeUnless(KotlinType::isError) ?: return null
        val psiType = member.psiTypeForNullabilityAnnotation ?: return null
        if (psiType is PsiPrimitiveType) return null

        if (kotlinType.isTypeParameter()) {
            if (!TypeUtils.hasNullableSuperType(kotlinType)) return NotNull::class.java.name
            if (!kotlinType.isMarkedNullable) return null
        }

        val nullability = kotlinType.nullability()
        return when (nullability) {
            TypeNullability.NOT_NULL -> NotNull::class.java.name
            TypeNullability.NULLABLE -> Nullable::class.java.name
            TypeNullability.FLEXIBLE -> null
        }
    }
}

fun DeclarationDescriptor.obtainLightAnnotations(
    ultraLightSupport: KtUltraLightSupport,
    parent: PsiElement
): List<KtLightAbstractAnnotation> = annotations.map { KtUltraLightAnnotationForDescriptor(it, ultraLightSupport, parent) }

class KtUltraLightAnnotationForDescriptor(
    private val annotationDescriptor: AnnotationDescriptor,
    private val ultraLightSupport: KtUltraLightSupport,
    parent: PsiElement
) : KtLightAbstractAnnotation(parent, { error("clsDelegate for annotation based on descriptor: $annotationDescriptor") }) {
    override fun getNameReferenceElement(): PsiJavaCodeReferenceElement? = null

    override fun getMetaData(): PsiMetaData? = null

    private val parameterList = ParameterListImpl()

    override fun getParameterList(): PsiAnnotationParameterList = parameterList

    override val kotlinOrigin: KtCallElement? get() = null

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(p0: String?, p1: T?) = cannotModify()

    override fun findAttributeValue(attributeName: String?): PsiAnnotationMemberValue? =
        PsiImplUtil.findAttributeValue(this, attributeName)

    override fun findDeclaredAttributeValue(attributeName: String?) =
        PsiImplUtil.findDeclaredAttributeValue(this, attributeName)

    override fun getQualifiedName() = annotationDescriptor.fqName?.asString()

    private inner class ParameterListImpl : KtLightElementBase(this@KtUltraLightAnnotationForDescriptor), PsiAnnotationParameterList {
        private val _attributes: Array<PsiNameValuePair> by lazyPub {
            annotationDescriptor.allValueArguments.map {
                PsiNameValuePairForAnnotationArgument(it.key.asString(), it.value, ultraLightSupport, this)
            }.toTypedArray()
        }

        override fun getAttributes(): Array<PsiNameValuePair> = _attributes

        override val kotlinOrigin: KtElement? get() = null
    }

    override fun getText() = "@$qualifiedName(" + parameterList.attributes.joinToString { it.name + "=" + it.value?.text } + ")"
}

private class PsiNameValuePairForAnnotationArgument(
    private val _name: String = "",
    private val constantValue: ConstantValue<*>,
    private val ultraLightSupport: KtUltraLightSupport,
    parent: PsiElement
) : KtLightElementBase(parent), PsiNameValuePair {
    override val kotlinOrigin: KtElement? get() = null

    private val _value by lazyPub {
        constantValue.toAnnotationMemberValue(this, ultraLightSupport)
    }

    override fun setValue(p0: PsiAnnotationMemberValue) = cannotModify()

    override fun getNameIdentifier() = LightIdentifier(parent.manager, _name)

    override fun getValue(): PsiAnnotationMemberValue? = _value

    override fun getLiteralValue(): String? = (value as? PsiLiteralExpression)?.value?.toString()

    override fun getName() = _name
}

private fun ConstantValue<*>.toAnnotationMemberValue(
    parent: PsiElement, ultraLightSupport: KtUltraLightSupport
): PsiAnnotationMemberValue? = when (this) {

    is AnnotationValue -> KtUltraLightAnnotationForDescriptor(value, ultraLightSupport, parent)

    is ArrayValue ->
        KtUltraLightPsiArrayInitializerMemberValue(lightParent = parent) { arrayLiteralParent ->
            this.value.mapNotNull { element -> element.toAnnotationMemberValue(arrayLiteralParent, ultraLightSupport) }
        }

    is ErrorValue -> null
    else -> createPsiLiteral(parent)
}

private fun ConstantValue<*>.createPsiLiteral(parent: PsiElement): PsiExpression? {
    val asString = asStringForPsiLiteral(parent)
    val instance = PsiElementFactory.SERVICE.getInstance(parent.project)
    return instance.createExpressionFromText(asString, parent)
}

private fun ConstantValue<*>.asStringForPsiLiteral(parent: PsiElement): String =
    when (this) {
        is NullValue -> "null"
        is StringValue -> "\"$value\""
        is KClassValue -> {
            val value = (value as KClassValue.Value.NormalClass).value
            val arrayPart = "[]".repeat(value.arrayNestedness)
            val fqName = value.classId.asSingleFqName()
            val canonicalText = psiType(
                fqName.asString(), parent, boxPrimitiveType = value.arrayNestedness > 0
            ).let(TypeConversionUtil::erasure).getCanonicalText(false)

            "$canonicalText$arrayPart.class"
        }
        is EnumValue -> "${enumClassId.asSingleFqName().asString()}.$enumEntryName"
        else -> value.toString()
    }


private class KtUltraLightPsiArrayInitializerMemberValue(
    val lightParent: PsiElement,
    private val arguments: (KtUltraLightPsiArrayInitializerMemberValue) -> List<PsiAnnotationMemberValue>
) : KtLightElementBase(lightParent), PsiArrayInitializerMemberValue {

    override val kotlinOrigin: KtElement? get() = null

    override fun getInitializers(): Array<PsiAnnotationMemberValue> = arguments(this).toTypedArray()

    override fun getParent(): PsiElement = lightParent
    override fun isPhysical(): Boolean = false

    override fun getText(): String = "{" + initializers.joinToString { it.text } + "}"
}

fun PsiModifierListOwner.isPrivateOrParameterInPrivateMethod(): Boolean {
    if (hasModifier(JvmModifier.PRIVATE)) return true
    if (this !is PsiParameter) return false
    val parentMethod = declarationScope as? PsiMethod ?: return false
    return parentMethod.hasModifier(JvmModifier.PRIVATE)
}
