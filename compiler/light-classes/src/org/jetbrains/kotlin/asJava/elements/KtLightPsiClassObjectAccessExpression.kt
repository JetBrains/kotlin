/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypes
import com.intellij.psi.impl.light.LightTypeElement
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KtLightPsiClassObjectAccessExpression(override val kotlinOrigin: KtClassLiteralExpression, lightParent: PsiElement) :
    KtLightPsiLiteral(kotlinOrigin, lightParent), PsiClassObjectAccessExpression {
    override fun getType(): PsiType {
        val bindingContext = LightClassGenerationSupport.getInstance(this.project).analyze(kotlinOrigin)
        val (classId, arrayDimensions) = bindingContext[BindingContext.COMPILE_TIME_VALUE, kotlinOrigin]
            ?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.safeAs<KClassValue>()?.value
            ?.safeAs<KClassValue.Value.NormalClass>()?.value ?: return PsiTypes.voidType()
        var type = psiType(classId.asSingleFqName().asString(), kotlinOrigin, boxPrimitiveType = arrayDimensions > 0)
        repeat(arrayDimensions) {
            type = type.createArrayType()
        }
        return type
    }

    override fun getOperand(): PsiTypeElement = LightTypeElement(kotlinOrigin.manager, type)
}
