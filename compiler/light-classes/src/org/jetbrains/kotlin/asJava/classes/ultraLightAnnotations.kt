package org.jetbrains.kotlin.asJava.classes

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightTypeElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.meta.PsiMetaData
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightNullabilityAnnotation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
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
        if (member.isPrivateOrParameterInPrivateMethod() || psiType is PsiPrimitiveType) return null

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
    ultraLightSupport: UltraLightSupport,
    parent: PsiElement
): List<KtLightAbstractAnnotation> = annotations.map { KtUltraLightAnnotationForDescriptor(it, ultraLightSupport, parent) }

class KtUltraLightAnnotationForDescriptor(
    private val annotationDescriptor: AnnotationDescriptor,
    private val ultraLightSupport: UltraLightSupport,
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
    private val ultraLightSupport: UltraLightSupport,
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
    parent: PsiElement, ultraLightSupport: UltraLightSupport
): PsiAnnotationMemberValue? = when (this) {

    is AnnotationValue -> KtUltraLightAnnotationForDescriptor(value, ultraLightSupport, parent)

    is ArrayValue ->
        KtUltraLightPsiArrayInitializerMemberValue(lightParent = parent) { arrayLiteralParent ->
            this.value.mapNotNull { element -> element.toAnnotationMemberValue(arrayLiteralParent, ultraLightSupport) }
        }

    is KClassValue -> KtUltraLightPsiClassObjectAccessExpression(this, ultraLightSupport, parent)

    is ErrorValue -> null

    else -> KtUltraLightPsiLiteralForConstantValue(this, ultraLightSupport, parent)
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

private open class KtUltraLightPsiLiteralForConstantValue(
    private val constantValue: ConstantValue<*>,
    private val ultraLightSupport: UltraLightSupport,
    lightParent: PsiElement
) : KtLightElementBase(lightParent), PsiLiteralExpression {
    override val kotlinOrigin: KtElement? get() = null

    override fun getValue(): Any? = constantValue.value

    override fun getType() =
        constantValue.getType(ultraLightSupport.moduleDescriptor).asPsiType(ultraLightSupport, TypeMappingMode.DEFAULT, this)

    override fun getText() = when (constantValue) {
        is NullValue -> "<undefined value>"
        is StringValue -> "\"${value.toString()}\""
        is EnumValue -> constantValue.enumClassId.shortClassName.asString() + "." + constantValue.enumEntryName.asString()
        else -> value.toString()
    }
}

private class KtUltraLightPsiClassObjectAccessExpression(
    kClassValue: KClassValue,
    ultraLightSupport: UltraLightSupport,
    parent: PsiElement
) : KtUltraLightPsiLiteralForConstantValue(kClassValue, ultraLightSupport, parent), PsiClassObjectAccessExpression {
    override fun getOperand(): PsiTypeElement {
        val argument = (type as? PsiClassReferenceType)?.parameters?.getOrNull(0)
        return LightTypeElement(parent.manager, argument ?: type)
    }

    override fun getText() = operand.text + ".class"
}

fun PsiModifierListOwner.isPrivateOrParameterInPrivateMethod(): Boolean {
    if (hasModifier(JvmModifier.PRIVATE)) return true
    if (this !is PsiParameter) return false
    val parentMethod = declarationScope as? PsiMethod ?: return false
    return parentMethod.hasModifier(JvmModifier.PRIVATE)
}
