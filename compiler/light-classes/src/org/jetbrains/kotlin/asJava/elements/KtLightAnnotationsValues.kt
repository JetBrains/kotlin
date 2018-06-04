/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.LanguageConstantExpressionEvaluator
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightTypeElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KtLightPsiArrayInitializerMemberValue(
    override val kotlinOrigin: KtElement,
    val lightParent: PsiElement,
    val arguments: (KtLightPsiArrayInitializerMemberValue) -> List<PsiAnnotationMemberValue>
) : KtLightElementBase(lightParent), PsiArrayInitializerMemberValue {
    override fun getInitializers(): Array<PsiAnnotationMemberValue> = arguments(this).toTypedArray()

    override fun getParent(): PsiElement = lightParent

    override fun isPhysical(): Boolean = false
}

open class KtLightPsiLiteral(
    override val kotlinOrigin: KtExpression,
    val lightParent: PsiElement
) : KtLightElementBase(lightParent), PsiLiteralExpression {

    override fun getValue(): Any? =
        LanguageConstantExpressionEvaluator.INSTANCE.forLanguage(kotlinOrigin.language)?.computeConstantExpression(this, false)

    override fun getType(): PsiType? {
        val bindingContext = LightClassGenerationSupport.getInstance(this.project).analyze(kotlinOrigin)
        val kotlinType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, kotlinOrigin] ?: return null
        return psiType(kotlinType, kotlinOrigin)
    }

    override fun getParent(): PsiElement = lightParent

    override fun isPhysical(): Boolean = false

    override fun replace(newElement: PsiElement): PsiElement {
        val value = (newElement as? PsiLiteral)?.value as? String ?: return this
        kotlinOrigin.replace(KtPsiFactory(this).createExpression("\"${StringUtil.escapeStringCharacters(value)}\""))
        return this
    }

    override fun getReference(): PsiReference? = references.singleOrNull()
    override fun getReferences(): Array<out PsiReference> = kotlinOrigin.references
}

class KtLightPsiClassObjectAccessExpression(override val kotlinOrigin: KtClassLiteralExpression, lightParent: PsiElement) :
    KtLightPsiLiteral(kotlinOrigin, lightParent), PsiClassObjectAccessExpression {
    override fun getType(): PsiType {
        val bindingContext = LightClassGenerationSupport.getInstance(this.project).analyze(kotlinOrigin)
        val kotlinType = bindingContext[BindingContext.COMPILE_TIME_VALUE, kotlinOrigin]
            ?.getValue(TypeUtils.NO_EXPECTED_TYPE).safeAs<SimpleType>() ?: return PsiType.VOID
        return psiType(kotlinType, kotlinOrigin) ?: return PsiType.VOID
    }

    override fun getOperand(): PsiTypeElement = LightTypeElement(kotlinOrigin.manager, type)
}

private fun psiType(kotlinType: KotlinType, context: PsiElement): PsiType? {
    val typeFqName = kotlinType.constructor.declarationDescriptor?.fqNameSafe?.asString() ?: return null
    return when (typeFqName) {
        "kotlin.Int" -> PsiType.INT
        "kotlin.Long" -> PsiType.LONG
        "kotlin.Short" -> PsiType.SHORT
        "kotlin.Boolean" -> PsiType.BOOLEAN
        "kotlin.Byte" -> PsiType.BYTE
        "kotlin.Char" -> PsiType.CHAR
        "kotlin.Double" -> PsiType.DOUBLE
        "kotlin.Float" -> PsiType.FLOAT
        "kotlin.String" -> PsiType.getJavaLangString(context.manager, GlobalSearchScope.projectScope(context.project))
        else -> PsiType.getTypeByName(typeFqName, context.project, context.resolveScope)
    }
}

class KtLightPsiNameValuePair private constructor(
    override val kotlinOrigin: KtElement,
    val valueArgument: KtValueArgument,
    lightParent: PsiElement
) : KtLightElementBase(lightParent),
    PsiNameValuePair {

    constructor(valueArgument: KtValueArgument, lightParent: PsiElement) : this(valueArgument.asElement(), valueArgument, lightParent)

    override fun setValue(newValue: PsiAnnotationMemberValue): PsiAnnotationMemberValue =
        throw UnsupportedOperationException("can't modify KtLightPsiNameValuePair")

    override fun getNameIdentifier(): PsiIdentifier? = LightIdentifier(kotlinOrigin.manager, valueArgument.name)

    override fun getName(): String? = valueArgument.getArgumentName()?.asName?.asString()

    override fun getValue(): PsiAnnotationMemberValue? =
        valueArgument.getArgumentExpression()?.let { convertToLightAnnotationMemberValue(this, it) }

    override fun getLiteralValue(): String? = (getValue() as? PsiLiteralExpression)?.value?.toString()

}