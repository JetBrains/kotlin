/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.classes.runReadAction
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.asJava.elements.LightParameter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.TypeUtils

fun computeExpression(expression: PsiElement): Any? {
    fun evalConstantValue(constantValue: ConstantValue<*>): Any? =
        if (constantValue is ArrayValue) {
            val items = constantValue.value.map { evalConstantValue(it) }
            items.singleOrNull() ?: items
        } else {
            constantValue.value
        }

    val expressionToCompute = when (expression) {
        is KtLightElementBase -> expression.kotlinOrigin as? KtExpression ?: return null
        else -> return null
    }

    val generationSupport = LightClassGenerationSupport.getInstance(expressionToCompute.project)
    val evaluator = generationSupport.createConstantEvaluator(expressionToCompute)

    val constant = runReadAction {
        val evaluatorTrace = DelegatingBindingTrace(generationSupport.analyze(expressionToCompute), "Evaluating annotation argument")
        evaluator.evaluateExpression(expressionToCompute, evaluatorTrace)
    } ?: return null

    if (constant.isError) return null
    return evalConstantValue(constant.toConstantValue(TypeUtils.NO_EXPECTED_TYPE))
}

fun fastCheckIsNullabilityApplied(lightElement: KtLightElement<*, PsiModifierListOwner>): Boolean {
    val elementIsApplicable = lightElement is KtLightMember<*> || lightElement is LightParameter
    if (!elementIsApplicable) return false

    val annotatedElement = lightElement.kotlinOrigin ?: return true

    // all data-class generated members are not-null
    if (annotatedElement is KtClass && annotatedElement.isData()) return true

    // backing fields for lateinit props are skipped
    if (lightElement is KtLightField && annotatedElement is KtProperty && annotatedElement.hasModifier(KtTokens.LATEINIT_KEYWORD)) return false

    if (lightElement is KtLightMethod && (annotatedElement as? KtModifierListOwner)?.isPrivate() == true) {
        return false
    }

    if (annotatedElement is KtParameter) {
        val containingClassOrObject = annotatedElement.containingClassOrObject
        if (containingClassOrObject?.isAnnotation() == true) return false
        if ((containingClassOrObject as? KtClass)?.isEnum() == true) {
            if (annotatedElement.parent.parent is KtPrimaryConstructor) return false
        }

        when (val parent = annotatedElement.parent.parent) {
            is KtConstructor<*> -> if (lightElement is KtLightParameter && parent.isPrivate()) return false
            is KtNamedFunction -> return !parent.isPrivate()
            is KtPropertyAccessor -> return (parent.parent as? KtProperty)?.isPrivate() != true
        }
    }

    return true
}