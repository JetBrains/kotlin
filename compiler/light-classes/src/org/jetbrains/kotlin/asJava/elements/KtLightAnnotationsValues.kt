/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightTypeElement
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.computeExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

open class KtLightPsiLiteral(
    override val kotlinOrigin: KtExpression,
    private val lightParent: PsiElement
) : KtLightElementBase(lightParent), PsiLiteralExpression {

    override fun getValue(): Any? = computeExpression(this)

    override fun getType(): PsiType? {
        val bindingContext = LightClassGenerationSupport.getInstance(this.project).analyze(kotlinOrigin)
        val kotlinType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, kotlinOrigin] ?: return null
        val typeFqName = kotlinType.constructor.declarationDescriptor?.fqNameSafe?.asString() ?: return null
        return psiType(typeFqName, kotlinOrigin)
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
        val (classId, arrayDimensions) = bindingContext[BindingContext.COMPILE_TIME_VALUE, kotlinOrigin]
            ?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.safeAs<KClassValue>()?.value
            ?.safeAs<KClassValue.Value.NormalClass>()?.value ?: return PsiType.VOID
        var type = psiType(classId.asSingleFqName().asString(), kotlinOrigin, boxPrimitiveType = arrayDimensions > 0)
        repeat(arrayDimensions) {
            type = type.createArrayType()
        }
        return type
    }

    override fun getOperand(): PsiTypeElement = LightTypeElement(kotlinOrigin.manager, type)
}
