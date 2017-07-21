/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
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

object CreateClassFromConstructorCallActionFactory: CreateClassFromUsageFactory<KtCallExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtCallExpression? {
        val diagElement = diagnostic.psiElement
        if (diagElement.getNonStrictParentOfType<KtTypeReference>() != null) return null

        val callExpr = diagElement.parent as? KtCallExpression ?: return null
        return if (callExpr.calleeExpression == diagElement) callExpr else null
    }

    override fun getPossibleClassKinds(element: KtCallExpression, diagnostic: Diagnostic): List<ClassKind> {
        val inAnnotationEntry = diagnostic.psiElement.getNonStrictParentOfType<KtAnnotationEntry>() != null

        val (context, moduleDescriptor) = element.analyzeFullyAndGetResult()
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
        val fullCallExpr =
                if (callParent is KtQualifiedExpression && callParent.selectorExpression == callExpr) callParent else callExpr

        val (context, moduleDescriptor) = callExpr.analyzeFullyAndGetResult()

        val call = callExpr.getCall(context) ?: return null
        val targetParents = getTargetParentsByCall(call, context).ifEmpty { return null }
        val inner = isInnerClassExpected(call)

        val valueArguments = callExpr.valueArguments
        val defaultParamName = if (inAnnotationEntry && valueArguments.size == 1) "value" else null
        val anyType = moduleDescriptor.builtIns.nullableAnyType
        val parameterInfos = valueArguments.map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
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

        return ClassInfo(
                name = name,
                targetParents = filteredParents,
                expectedTypeInfo = expectedTypeInfo,
                inner = inner,
                typeArguments = typeArgumentInfos,
                parameterInfos = parameterInfos
        )
    }
}
