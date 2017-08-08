/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Key
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.configuration.ui.NotPropertyListPanel
import org.jetbrains.kotlin.idea.core.NotPropertiesService
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.shouldNotConvertToProperty
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isUnit
import javax.swing.JComponent

class UsePropertyAccessSyntaxInspection : IntentionBasedInspection<KtCallExpression>(UsePropertyAccessSyntaxIntention::class), CleanupLocalInspectionTool {

    val fqNameList = mutableListOf<FqNameUnsafe>()

    private var fqNameStrings: List<String>
        get() = fqNameList.map { it.asString() }
        set(value) {
            fqNameList.clear()
            value.mapTo(fqNameList, ::FqNameUnsafe)
        }

    init {
        fqNameStrings = NotPropertiesServiceImpl.default
    }

    override fun createOptionsPanel(): JComponent? {
        val list = NotPropertyListPanel(fqNameList)
        return LabeledComponent.create(list, "Excluded methods")
    }
}


class NotPropertiesServiceImpl(private val project: Project) : NotPropertiesService {
    override fun getNotProperties(element: PsiElement): Set<FqNameUnsafe> {
        val profile = InspectionProjectProfileManager.getInstance(project).inspectionProfile
        val tool = profile.getUnwrappedTool(USE_PROPERTY_ACCESS_INSPECTION, element)
        return (tool?.fqNameList ?: default.map(::FqNameUnsafe)).toSet()
    }

    companion object {

        val default = listOf(
                "java.net.Socket.getInputStream",
                "java.net.Socket.getOutputStream",
                "java.net.URLConnection.getInputStream",
                "java.net.URLConnection.getOutputStream",
                "java.util.concurrent.atomic.AtomicInteger.getAndIncrement",
                "java.util.concurrent.atomic.AtomicInteger.getAndDecrement",
                "java.util.concurrent.atomic.AtomicLong.getAndIncrement",
                "java.util.concurrent.atomic.AtomicLong.getAndDecrement"
        )


        val USE_PROPERTY_ACCESS_INSPECTION: Key<UsePropertyAccessSyntaxInspection> = Key.create("UsePropertyAccessSyntax")
    }
}

class UsePropertyAccessSyntaxIntention : SelfTargetingOffsetIndependentIntention<KtCallExpression>(KtCallExpression::class.java, "Use property access syntax") {
    override fun isApplicableTo(element: KtCallExpression): Boolean {
        return detectPropertyNameToUse(element) != null
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        applyTo(element, detectPropertyNameToUse(element)!!)
    }

    fun applyTo(element: KtCallExpression, propertyName: Name): KtExpression {
        val arguments = element.valueArguments
        return when (arguments.size) {
            0 -> replaceWithPropertyGet(element, propertyName)
            1 -> replaceWithPropertySet(element, propertyName)
            else -> error("More than one argument in call to accessor")
        }
    }

    fun detectPropertyNameToUse(callExpression: KtCallExpression): Name? {
        if (callExpression.getQualifiedExpressionForSelector()?.receiverExpression is KtSuperExpression) return null // cannot call extensions on "super"

        val callee = callExpression.calleeExpression as? KtNameReferenceExpression ?: return null

        val resolutionFacade = callExpression.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(callExpression, BodyResolveMode.PARTIAL_FOR_COMPLETION)
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
        if (!resolvedCall.isReallySuccess()) return null

        val function = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return null

        val notProperties = (inspection as? UsePropertyAccessSyntaxInspection)?.fqNameList?.toSet() ?:
                            NotPropertiesService.getNotProperties(callExpression)
        if (function.shouldNotConvertToProperty(notProperties)) return null

        val resolutionScope = callExpression.getResolutionScope(bindingContext, resolutionFacade)
        val property = findSyntheticProperty(function, resolutionFacade.getFrontendService(SyntheticScopes::class.java)) ?: return null

        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(callee)
        val qualifiedExpression = callExpression.getQualifiedExpressionForSelectorOrThis()
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, qualifiedExpression] ?: TypeUtils.NO_EXPECTED_TYPE

        if (!checkWillResolveToProperty(resolvedCall, property, bindingContext, resolutionScope, dataFlowInfo, expectedType, resolutionFacade)) return null

        val isSetUsage = callExpression.valueArguments.size == 1

        if (isSetUsage && qualifiedExpression.isUsedAsExpression(bindingContext)) {
            // call to the setter used as expression can be converted in the only case when it's used as body expression for some declaration and its type is Unit
            val parent = qualifiedExpression.parent
            if (parent !is KtDeclarationWithBody || qualifiedExpression != parent.bodyExpression) return null
            if (function.returnType?.isUnit() != true) return null
        }

        if (isSetUsage && property.type != function.valueParameters.single().type) {
            val qualifiedExpressionCopy = qualifiedExpression.copied()
            val callExpressionCopy = ((qualifiedExpressionCopy as? KtQualifiedExpression)?.selectorExpression ?: qualifiedExpressionCopy) as KtCallExpression
            val newExpression = applyTo(callExpressionCopy, property.name)
            val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace")
            val newBindingContext = newExpression.analyzeInContext(
                    resolutionScope,
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
            override fun getFunctionLiteralArguments(): List<LambdaArgument> = emptyList()
        }

        val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace")
        val context = BasicCallResolutionContext.create(bindingTrace, resolutionScope, newCall, expectedType, dataFlowInfo,
                                                        ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                                                        false)
        val callResolver = facade.frontendService<CallResolver>()
        val result = callResolver.resolveSimpleProperty(context)
        return result.isSuccess && result.resultingDescriptor.original == property
    }

    private fun findSyntheticProperty(function: FunctionDescriptor, syntheticScopes: SyntheticScopes): SyntheticJavaPropertyDescriptor? {
        SyntheticJavaPropertyDescriptor.findByGetterOrSetter(function, syntheticScopes)?.let { return it }

        for (overridden in function.overriddenDescriptors) {
            findSyntheticProperty(overridden, syntheticScopes)?.let { return it }
        }

        return null
    }

    private fun replaceWithPropertyGet(callExpression: KtCallExpression, propertyName: Name): KtExpression {
        val newExpression = KtPsiFactory(callExpression).createExpression(propertyName.render())
        return callExpression.replaced(newExpression)
    }

    private fun replaceWithPropertySet(callExpression: KtCallExpression, propertyName: Name): KtExpression {
        val call = callExpression.getQualifiedExpressionForSelector() ?: callExpression
        val callParent = call.parent
        var callToConvert = callExpression
        if (callParent is KtDeclarationWithBody && call == callParent.bodyExpression) {
            ConvertToBlockBodyIntention.convert(callParent)
            val firstStatement = (callParent.bodyExpression as? KtBlockExpression)?.statements?.first()
            callToConvert = (firstStatement as? KtQualifiedExpression)?.selectorExpression as? KtCallExpression
                ?: firstStatement as? KtCallExpression
                ?: throw IllegalStateException("Unexpected contents of function after conversion: ${callParent.text}")
        }

        val qualifiedExpression = callToConvert.getQualifiedExpressionForSelector()
        val argument = callToConvert.valueArguments.single()
        if (qualifiedExpression != null) {
            val pattern = when (qualifiedExpression) {
                is KtDotQualifiedExpression -> "$0.$1=$2"
                is KtSafeQualifiedExpression -> "$0?.$1=$2"
                else -> error(qualifiedExpression) //TODO: make it sealed?
            }
            val newExpression = KtPsiFactory(callToConvert).createExpressionByPattern(
                    pattern,
                    qualifiedExpression.receiverExpression,
                    propertyName,
                    argument.getArgumentExpression()!!
            )
            return qualifiedExpression.replaced(newExpression)
        }
        else {
            val newExpression = KtPsiFactory(callToConvert).createExpressionByPattern("$0=$1", propertyName, argument.getArgumentExpression()!!)
            return callToConvert.replaced(newExpression)
        }
    }
}