/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getTypeInfoForTypeArguments
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

object CreateClassFromConstructorCallActionFactory : CreateClassFromUsageFactory<KtCallExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtCallExpression? {
        val diagElement = diagnostic.psiElement
        if (diagElement.getNonStrictParentOfType<KtTypeReference>() != null) return null

        val callExpr = diagElement.parent as? KtCallExpression ?: return null
        return if (callExpr.calleeExpression == diagElement) callExpr else null
    }

    override fun getPossibleClassKinds(element: KtCallExpression, diagnostic: Diagnostic): List<ClassKind> {
        val inAnnotationEntry = diagnostic.psiElement.getNonStrictParentOfType<KtAnnotationEntry>() != null

        val (context, moduleDescriptor) = element.analyzeAndGetResult()
        val call = element.getCall(context) ?: return emptyList()
        val targetParents = getTargetParentsByCall(call, context).ifEmpty { return emptyList() }

        val classKind = if (inAnnotationEntry) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS
        val fullCallExpr = element.getQualifiedExpressionForSelectorOrThis()
        val expectedType = fullCallExpr.guessTypeForClass(context, moduleDescriptor)
        if (expectedType != null && !targetParents.any { getClassKindFilter(expectedType, it)(classKind) }) return emptyList()

        return listOf(classKind)
    }

    override fun extractFixData(element: KtCallExpression, diagnostic: Diagnostic): ClassInfo? {
        val diagElement = diagnostic.psiElement
        if (diagElement.getNonStrictParentOfType<KtTypeReference>() != null) return null

        val inAnnotationEntry = diagElement.getNonStrictParentOfType<KtAnnotationEntry>() != null

        val callExpr = diagElement.parent as? KtCallExpression ?: return null
        if (callExpr.calleeExpression != diagElement) return null

        val calleeExpr = callExpr.calleeExpression as? KtSimpleNameExpression ?: return null

        val name = calleeExpr.getReferencedName()
        if (!inAnnotationEntry && !name.checkClassName()) return null

        val callParent = callExpr.parent
        val fullCallExpr = if (callParent is KtQualifiedExpression && callParent.selectorExpression == callExpr) callParent else callExpr

        val (context, moduleDescriptor) = callExpr.analyzeAndGetResult()

        val call = callExpr.getCall(context) ?: return null
        val targetParents = getTargetParentsByCall(call, context).ifEmpty { return null }
        val inner = isInnerClassExpected(call)

        val valueArguments = callExpr.valueArguments
        val defaultParamName = if (inAnnotationEntry && valueArguments.size == 1) "value" else null
        val anyType = moduleDescriptor.builtIns.nullableAnyType
        val parameterInfos = valueArguments.map {
            ParameterInfo(
                it.getArgumentExpression()?.let { expression -> TypeInfo(expression, Variance.IN_VARIANCE) } ?: TypeInfo(
                    anyType,
                    Variance.IN_VARIANCE
                ),
                it.getArgumentName()?.referenceExpression?.getReferencedName() ?: defaultParamName
            )
        }

        val classKind = if (inAnnotationEntry) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS

        val expectedType = fullCallExpr.guessTypeForClass(context, moduleDescriptor)
        val expectedTypeInfo = expectedType?.toClassTypeInfo() ?: TypeInfo.Empty
        val filteredParents = if (expectedType != null) {
            targetParents.filter { getClassKindFilter(expectedType, it)(classKind) }.ifEmpty { return null }
        } else targetParents

        val typeArgumentInfos = if (inAnnotationEntry) Collections.emptyList() else callExpr.getTypeInfoForTypeArguments()

        val argumentClassVisibilities = valueArguments.mapNotNull {
            (it.getArgumentExpression()?.getCallableDescriptor() as? ClassConstructorDescriptor)?.containingDeclaration?.visibility
        }
        val primaryConstructorVisibility = when {
            Visibilities.PRIVATE in argumentClassVisibilities -> Visibilities.PRIVATE
            Visibilities.INTERNAL in argumentClassVisibilities -> Visibilities.INTERNAL
            else -> null
        }

        return ClassInfo(
            name = name,
            targetParents = filteredParents,
            expectedTypeInfo = expectedTypeInfo,
            inner = inner,
            typeArguments = typeArgumentInfos,
            parameterInfos = parameterInfos,
            primaryConstructorVisibility = primaryConstructorVisibility
        )
    }
}
