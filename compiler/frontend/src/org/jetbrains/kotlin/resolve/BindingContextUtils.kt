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

package org.jetbrains.kotlin.resolve.bindingContextUtil

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.takeSnapshot
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.noTypeInfo
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

fun KtReturnExpression.getTargetFunctionDescriptor(context: BindingContext): FunctionDescriptor? {
    val targetLabel = getTargetLabel()
    if (targetLabel != null) return context[LABEL_TARGET, targetLabel]?.let { context[FUNCTION, it] }

    val declarationDescriptor = context[DECLARATION_TO_DESCRIPTOR, getNonStrictParentOfType<KtDeclarationWithBody>()]
    val containingFunctionDescriptor = DescriptorUtils.getParentOfType(declarationDescriptor, FunctionDescriptor::class.java, false)
    if (containingFunctionDescriptor == null) return null

    return generateSequence(containingFunctionDescriptor) { DescriptorUtils.getParentOfType(it, FunctionDescriptor::class.java) }
            .dropWhile { it is AnonymousFunctionDescriptor }
            .firstOrNull()
}

fun KtReturnExpression.getTargetFunction(context: BindingContext): KtCallableDeclaration? {
    return getTargetFunctionDescriptor(context)?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtCallableDeclaration }
}

fun KtExpression.isUsedAsExpression(context: BindingContext): Boolean = context[BindingContext.USED_AS_EXPRESSION, this]!!
fun KtExpression.isUsedAsResultOfLambda(context: BindingContext): Boolean = context[BindingContext.USED_AS_RESULT_OF_LAMBDA, this]!!
fun KtExpression.isUsedAsStatement(context: BindingContext): Boolean = !isUsedAsExpression(context)


fun <C : ResolutionContext<C>> ResolutionContext<C>.recordDataFlowInfo(expression: KtExpression?) {
    if (expression == null) return

    val typeInfo = trace.get(BindingContext.EXPRESSION_TYPE_INFO, expression)
    if (typeInfo != null) {
        trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, typeInfo.replaceDataFlowInfo(dataFlowInfo))
    }
    else if (dataFlowInfo != DataFlowInfo.EMPTY) {
        // Don't store anything in BindingTrace if it's simply an empty DataFlowInfo
        trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, noTypeInfo(dataFlowInfo))
    }
}

fun BindingTrace.recordScope(scope: LexicalScope, element: KtElement?) {
    if (element != null) {
        record(BindingContext.LEXICAL_SCOPE, element, scope.takeSnapshot() as LexicalScope)
    }
}

fun BindingContext.getDataFlowInfoAfter(position: PsiElement): DataFlowInfo {
    for (element in position.parentsWithSelf) {
        (element as? KtExpression)?.let {
            val parent = it.parent
            //TODO: it's a hack because KotlinTypeInfo with wrong DataFlowInfo stored for call expression after qualifier
            if (parent is KtQualifiedExpression && it == parent.selectorExpression) return@let null
            this[BindingContext.EXPRESSION_TYPE_INFO, it]
        }?.let { return it.dataFlowInfo }
    }
    return DataFlowInfo.EMPTY
}

fun BindingContext.getDataFlowInfoBefore(position: PsiElement): DataFlowInfo {
    for (element in position.parentsWithSelf) {
        (element as? KtExpression)
                ?.let { this[BindingContext.DATA_FLOW_INFO_BEFORE, it] }
                ?.let { return it }
    }
    return DataFlowInfo.EMPTY
}

fun KtExpression.isUnreachableCode(context: BindingContext): Boolean = context[BindingContext.UNREACHABLE_CODE, this]!!

fun KtExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = if (this is KtReferenceExpression) context[BindingContext.REFERENCE_TARGET, this] else null
    return targetDescriptor?.let { listOf(it) } ?: context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
}

fun KtTypeReference.getAbbreviatedTypeOrType(context: BindingContext) =
        context[BindingContext.ABBREVIATED_TYPE, this] ?: context[BindingContext.TYPE, this]

fun KtTypeElement.getAbbreviatedTypeOrType(context: BindingContext): KotlinType? {
    val parent = parent
    return when (parent) {
        is KtTypeReference -> parent.getAbbreviatedTypeOrType(context)
        is KtNullableType -> {
            val outerType = parent.getAbbreviatedTypeOrType(context)
            if (this is KtNullableType) outerType else outerType?.makeNotNullable()
        }
        else -> null
    }
}