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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.BindingContext.FUNCTION
import org.jetbrains.kotlin.resolve.BindingContext.LABEL_TARGET
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.asKtScope
import org.jetbrains.kotlin.resolve.scopes.utils.takeSnapshot
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.noTypeInfo
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

public operator fun <K, V: Any> BindingContext.get(slice: ReadOnlySlice<K, V>, key: K): V? = get(slice, key)

public fun KtReturnExpression.getTargetFunctionDescriptor(context: BindingContext): FunctionDescriptor? {
    val targetLabel = getTargetLabel()
    if (targetLabel != null) return context[LABEL_TARGET, targetLabel]?.let { context[FUNCTION, it] }

    val declarationDescriptor = context[DECLARATION_TO_DESCRIPTOR, getNonStrictParentOfType<KtDeclarationWithBody>()]
    val containingFunctionDescriptor = DescriptorUtils.getParentOfType(declarationDescriptor, javaClass<FunctionDescriptor>(), false)
    if (containingFunctionDescriptor == null) return null

    return sequence(containingFunctionDescriptor) { DescriptorUtils.getParentOfType(it, javaClass<FunctionDescriptor>()) }
            .dropWhile { it is AnonymousFunctionDescriptor }
            .firstOrNull()
}

public fun KtReturnExpression.getTargetFunction(context: BindingContext): KtCallableDeclaration? {
    return getTargetFunctionDescriptor(context)?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtCallableDeclaration }
}

public fun KtExpression.isUsedAsExpression(context: BindingContext): Boolean = context[BindingContext.USED_AS_EXPRESSION, this]!!
public fun KtExpression.isUsedAsStatement(context: BindingContext): Boolean = !isUsedAsExpression(context)


public fun <C : ResolutionContext<C>> ResolutionContext<C>.recordDataFlowInfo(expression: KtExpression?) {
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

public fun BindingTrace.recordScope(scope: LexicalScope, element: KtElement?) {
    if (element == null) return

    val scopeToRecord = scope.takeSnapshot()
    record(BindingContext.LEXICAL_SCOPE, element, scopeToRecord)

    // todo: remove it later
    if (element is KtExpression) {
        record(BindingContext.RESOLUTION_SCOPE, element, scopeToRecord.asKtScope())
    }
}

public fun BindingContext.getDataFlowInfo(expression: KtExpression?): DataFlowInfo =
    expression?.let { this[BindingContext.EXPRESSION_TYPE_INFO, it]?.dataFlowInfo } ?: DataFlowInfo.EMPTY

public fun KtExpression.isUnreachableCode(context: BindingContext): Boolean = context[BindingContext.UNREACHABLE_CODE, this]!!

public fun KtExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = if (this is KtReferenceExpression) context[BindingContext.REFERENCE_TARGET, this] else null
    return targetDescriptor?.let { listOf(it) } ?: context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
}