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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
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
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

fun KtReturnExpression.getTargetFunctionDescriptor(context: BindingContext): FunctionDescriptor? {
    val targetLabel = getTargetLabel()
    if (targetLabel != null) return context[LABEL_TARGET, targetLabel]?.let { context[FUNCTION, it] }

    val declarationDescriptor = context[DECLARATION_TO_DESCRIPTOR, getNonStrictParentOfType<KtDeclarationWithBody>()]
    val containingFunctionDescriptor = DescriptorUtils.getParentOfType(declarationDescriptor, FunctionDescriptor::class.java, false)
        ?: return null

    return generateSequence(containingFunctionDescriptor) { DescriptorUtils.getParentOfType(it, FunctionDescriptor::class.java) }
        .dropWhile { it is AnonymousFunctionDescriptor }
        .firstOrNull()
}

fun KtReturnExpression.getTargetFunction(context: BindingContext): KtCallableDeclaration? {
    return getTargetFunctionDescriptor(context)?.let { DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtCallableDeclaration }
}

fun KtExpression.isUsedAsExpression(context: BindingContext): Boolean =
    context[USED_AS_EXPRESSION, this]
        ?: throw AssertionError(
            "BindingContext returned null for Boolean slice: " +
                    if (context == EMPTY) "BindingContext.EMPTY" else context.javaClass.toString()
        )

fun KtExpression.isUsedAsResultOfLambda(context: BindingContext): Boolean = context[USED_AS_RESULT_OF_LAMBDA, this]!!
fun KtExpression.isUsedAsStatement(context: BindingContext): Boolean = !isUsedAsExpression(context)


fun <C : ResolutionContext<C>> ResolutionContext<C>.recordDataFlowInfo(expression: KtExpression?) {
    if (expression == null) return

    val typeInfo = trace.get(EXPRESSION_TYPE_INFO, expression)
    if (typeInfo != null) {
        trace.record(EXPRESSION_TYPE_INFO, expression, typeInfo.replaceDataFlowInfo(dataFlowInfo))
    } else if (dataFlowInfo != DataFlowInfo.EMPTY) {
        // Don't store anything in BindingTrace if it's simply an empty DataFlowInfo
        trace.record(EXPRESSION_TYPE_INFO, expression, noTypeInfo(dataFlowInfo))
    }
}

fun BindingTrace.recordScope(scope: LexicalScope, element: KtElement?) {
    if (element != null) {
        record(LEXICAL_SCOPE, element, scope.takeSnapshot() as LexicalScope)
    }
}

fun BindingContext.getDataFlowInfoAfter(position: PsiElement): DataFlowInfo {
    for (element in position.parentsWithSelf) {
        (element as? KtExpression)?.let {
            val parent = it.parent
            //TODO: it's a hack because KotlinTypeInfo with wrong DataFlowInfo stored for call expression after qualifier
            if (parent is KtQualifiedExpression && it == parent.selectorExpression) return@let null
            this[EXPRESSION_TYPE_INFO, it]
        }?.let { return it.dataFlowInfo }
    }
    return DataFlowInfo.EMPTY
}

fun BindingContext.getDataFlowInfoBefore(position: PsiElement): DataFlowInfo {
    for (element in position.parentsWithSelf) {
        (element as? KtExpression)
            ?.let { this[DATA_FLOW_INFO_BEFORE, it] }
            ?.let { return it }
    }
    return DataFlowInfo.EMPTY
}

fun KtExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = if (this is KtReferenceExpression) context[REFERENCE_TARGET, this] else null
    return targetDescriptor?.let { listOf(it) } ?: context[AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
}

fun KtTypeReference.getAbbreviatedTypeOrType(context: BindingContext) =
    context[ABBREVIATED_TYPE, this] ?: context[TYPE, this]

fun KtTypeElement.getAbbreviatedTypeOrType(context: BindingContext): KotlinType? {
    return when (val parent = parent) {
        is KtTypeReference -> parent.getAbbreviatedTypeOrType(context)
        is KtNullableType -> {
            val outerType = parent.getAbbreviatedTypeOrType(context)
            if (this is KtNullableType) outerType else outerType?.makeNotNullable()
        }
        else -> null
    }
}

fun <T : PsiElement> KtElement.getParentOfTypeCodeFragmentAware(vararg parentClasses: Class<out T>): T? {
    PsiTreeUtil.getParentOfType(this, *parentClasses)?.let { return it }

    val containingFile = this.containingFile
    if (containingFile is KtCodeFragment) {
        val context = containingFile.context
        if (context != null) {
            return PsiTreeUtil.getParentOfType(context, *parentClasses)
        }
    }

    return null
}

fun getEnclosingDescriptor(context: BindingContext, element: KtElement): DeclarationDescriptor {
    val declaration =
        element.getParentOfTypeCodeFragmentAware(KtNamedDeclaration::class.java)
            ?: throw KotlinExceptionWithAttachments("No parent KtNamedDeclaration for of type ${element.javaClass}")
                .withAttachment("element.kt", element.text)
    return if (declaration is KtFunctionLiteral) {
        getEnclosingDescriptor(context, declaration)
    } else {
        context.get(DECLARATION_TO_DESCRIPTOR, declaration)
            ?: throw KotlinExceptionWithAttachments("No descriptor for named declaration of type ${declaration.javaClass}")
                .withAttachment("declaration.kt", declaration.text)
    }
}

fun getEnclosingFunctionDescriptor(context: BindingContext, element: KtElement): FunctionDescriptor? {
    val functionOrClass = element.getParentOfTypeCodeFragmentAware(KtFunction::class.java, KtClassOrObject::class.java)
    val descriptor = context.get(DECLARATION_TO_DESCRIPTOR, functionOrClass)
    return if (functionOrClass is KtFunction) {
        if (descriptor is FunctionDescriptor) descriptor else null
    } else {
        if (descriptor is ClassDescriptor) descriptor.unsubstitutedPrimaryConstructor else null
    }
}