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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.asKtScope
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

class UsePropertyAccessSyntaxInspection : IntentionBasedInspection<KtCallExpression>(UsePropertyAccessSyntaxIntention()), CleanupLocalInspectionTool

class UsePropertyAccessSyntaxIntention : JetSelfTargetingOffsetIndependentIntention<KtCallExpression>(javaClass(), "Use property access syntax") {
    override fun isApplicableTo(element: KtCallExpression): Boolean {
        return detectPropertyNameToUse(element) != null
    }

    override fun applyTo(element: KtCallExpression, editor: Editor) {
        applyTo(element, detectPropertyNameToUse(element)!!)
    }

    public fun applyTo(element: KtCallExpression, propertyName: Name): KtExpression {
        val arguments = element.getValueArguments()
        return when (arguments.size()) {
            0 -> replaceWithPropertyGet(element, propertyName)
            1 -> replaceWithPropertySet(element, propertyName, arguments.single())
            else -> error("More than one argument in call to accessor")
        }
    }

    public fun detectPropertyNameToUse(callExpression: KtCallExpression): Name? {
        if (callExpression.getQualifiedExpressionForSelector()?.getReceiverExpression() is KtSuperExpression) return null // cannot call extensions on "super"

        val callee = callExpression.getCalleeExpression() as? KtNameReferenceExpression ?: return null

        val resolutionFacade = callExpression.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(callExpression, BodyResolveMode.PARTIAL)
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
        if (!resolvedCall.isReallySuccess()) return null

        val function = resolvedCall.getResultingDescriptor() as? FunctionDescriptor ?: return null
        val resolutionScope = callExpression.getResolutionScope(bindingContext, resolutionFacade)
        val property = findSyntheticProperty(function, resolutionScope.asKtScope()) ?: return null

        val dataFlowInfo = bindingContext.getDataFlowInfo(callee)
        val qualifiedExpression = callExpression.getQualifiedExpressionForSelectorOrThis()
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, qualifiedExpression] ?: TypeUtils.NO_EXPECTED_TYPE

        if (!checkWillResolveToProperty(resolvedCall, property, bindingContext, resolutionScope, dataFlowInfo, expectedType, resolutionFacade)) return null

        val isSetUsage = callExpression.valueArguments.size() == 1
        if (isSetUsage && property.type != function.valueParameters.single().type) {
            val qualifiedExpressionCopy = qualifiedExpression.copied()
            val callExpressionCopy = ((qualifiedExpressionCopy as? KtQualifiedExpression)?.selectorExpression ?: qualifiedExpressionCopy) as KtCallExpression
            val newExpression = applyTo(callExpressionCopy, property.name)
            val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace")
            val newBindingContext = newExpression.analyzeInContext(
                    resolutionScope.asKtScope(),
                    contextExpression = callExpression,
                    trace = bindingTrace,
                    dataFlowInfo = dataFlowInfo,
                    expectedType = expectedType,
                    isStatement = true
            )
            if (newBindingContext.diagnostics.any { it.severity == Severity.ERROR }) return null
        }

        return property.name
    }

    private fun checkWillResolveToProperty(
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            property: SyntheticJavaPropertyDescriptor,
            bindingContext: BindingContext,
            resolutionScope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            expectedType: KotlinType,
            facade: ResolutionFacade
    ): Boolean {
        val project = resolvedCall.call.callElement.project
        val newCall = object : DelegatingCall(resolvedCall.call) {
            private val newCallee = KtPsiFactory(project).createExpressionByPattern("$0", property.name)

            override fun getCalleeExpression() = newCallee
            override fun getValueArgumentList(): KtValueArgumentList? = null
            override fun getValueArguments(): List<ValueArgument> = emptyList()
            override fun getFunctionLiteralArguments(): List<FunctionLiteralArgument> = emptyList()
        }

        val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace")
        val context = BasicCallResolutionContext.create(bindingTrace, resolutionScope, newCall, expectedType, dataFlowInfo,
                                                        ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                                                        CallChecker.DoNothing, false)
        val callResolver = facade.frontendService<CallResolver>()
        val result = callResolver.resolveSimpleProperty(context)
        return result.isSuccess && result.resultingDescriptor.original == property
    }

    private fun findSyntheticProperty(function: FunctionDescriptor, resolutionScope: KtScope): SyntheticJavaPropertyDescriptor? {
        SyntheticJavaPropertyDescriptor.findByGetterOrSetter(function, resolutionScope)?.let { return it }

        for (overridden in function.getOverriddenDescriptors()) {
            findSyntheticProperty(overridden, resolutionScope)?.let { return it }
        }

        return null
    }

    private fun replaceWithPropertyGet(callExpression: KtCallExpression, propertyName: Name): KtExpression {
        val newExpression = KtPsiFactory(callExpression).createExpression(propertyName.render())
        return callExpression.replaced(newExpression)
    }

    //TODO: what if it was used as expression (of type Unit)?
    private fun replaceWithPropertySet(callExpression: KtCallExpression, propertyName: Name, argument: KtValueArgument): KtExpression {
        val qualifiedExpression = callExpression.getQualifiedExpressionForSelector()
        if (qualifiedExpression != null) {
            val pattern = when (qualifiedExpression) {
                is KtDotQualifiedExpression -> "$0.$1=$2"
                is KtSafeQualifiedExpression -> "$0?.$1=$2"
                else -> error(qualifiedExpression) //TODO: make it sealed?
            }
            val newExpression = KtPsiFactory(callExpression).createExpressionByPattern(
                    pattern,
                    qualifiedExpression.getReceiverExpression(),
                    propertyName,
                    argument.getArgumentExpression()!!
            )
            return qualifiedExpression.replaced(newExpression)
        }
        else {
            val newExpression = KtPsiFactory(callExpression).createExpressionByPattern("$0=$1", propertyName, argument.getArgumentExpression()!!)
            return callExpression.replaced(newExpression)
        }
    }
}