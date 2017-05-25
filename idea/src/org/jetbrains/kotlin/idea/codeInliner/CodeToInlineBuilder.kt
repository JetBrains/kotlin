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

package org.jetbrains.kotlin.idea.codeInliner

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.asExpression
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.intentions.SpecifyExplicitLambdaSignatureIntention
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.sure
import java.util.*

class CodeToInlineBuilder(
        private val targetCallable: CallableDescriptor,
        private val resolutionFacade: ResolutionFacade
) {
    private val psiFactory = KtPsiFactory(resolutionFacade.project)

    //TODO: document that code will be modified
    fun prepareCodeToInline(
            mainExpression: KtExpression?,
            statementsBefore: List<KtExpression>,
            analyze: () -> BindingContext
    ): CodeToInline {
        var bindingContext = analyze()

        val codeToInline = MutableCodeToInline(mainExpression, statementsBefore.toMutableList(), mutableSetOf())

        bindingContext = insertExplicitTypeArguments(codeToInline, bindingContext, analyze)

        insertExplicitReceivers(codeToInline, bindingContext)

        // NB: we must check in codeToInline, otherwise we get AE from addPostInsertionAction
        if (mainExpression != null && mainExpression in codeToInline) {
            val functionLiteralExpression = mainExpression.unpackFunctionLiteral(true)
            if (functionLiteralExpression != null) {
                val functionLiteralParameterTypes = getParametersForFunctionLiteral(functionLiteralExpression, bindingContext)
                if (functionLiteralParameterTypes != null) {
                    codeToInline.addPostInsertionAction(mainExpression) { inlinedExpression ->
                        addFunctionLiteralParameterTypes(functionLiteralParameterTypes, inlinedExpression)
                    }
                }
            }
        }

        return codeToInline.toNonMutable()
    }

    private fun getParametersForFunctionLiteral(functionLiteralExpression: KtLambdaExpression, context: BindingContext): String? {
        val lambdaDescriptor = context.get(BindingContext.FUNCTION, functionLiteralExpression.functionLiteral)
        if (lambdaDescriptor == null || ErrorUtils.containsErrorType(lambdaDescriptor)) return null
        return lambdaDescriptor.valueParameters.joinToString {
            it.name.render() + ": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(it.type)
        }
    }

    private fun addFunctionLiteralParameterTypes(parameters: String, inlinedExpression: KtExpression) {
        val containingFile = inlinedExpression.containingKtFile
        val resolutionFacade = containingFile.getResolutionFacade()

        val lambdaExpr = inlinedExpression.unpackFunctionLiteral(true).sure {
            "can't find function literal expression for " + inlinedExpression.text
        }
        if (!needToAddParameterTypes(lambdaExpr, resolutionFacade)) return
        SpecifyExplicitLambdaSignatureIntention.applyWithParameters(lambdaExpr, parameters)
    }

    private fun needToAddParameterTypes(
            lambdaExpression: KtLambdaExpression,
            resolutionFacade: ResolutionFacade
    ): Boolean {
        val functionLiteral = lambdaExpression.functionLiteral
        val context = resolutionFacade.analyze(lambdaExpression, BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        return context.diagnostics.any { diagnostic ->
            val factory = diagnostic.factory
            val element = diagnostic.psiElement
            val hasCantInferParameter = factory == Errors.CANNOT_INFER_PARAMETER_TYPE &&
                                        element.parent.parent == functionLiteral
            val hasUnresolvedItOrThis = factory == Errors.UNRESOLVED_REFERENCE &&
                                        element.text == "it" &&
                                        element.getStrictParentOfType<KtFunctionLiteral>() == functionLiteral
            hasCantInferParameter || hasUnresolvedItOrThis
        }
    }

    private fun insertExplicitTypeArguments(codeToInline: MutableCodeToInline, bindingContext: BindingContext, analyze: () -> BindingContext): BindingContext {
        val typeArgsToAdd = ArrayList<Pair<KtCallExpression, KtTypeArgumentList>>()
        codeToInline.forEachDescendantOfType<KtCallExpression> {
            if (InsertExplicitTypeArgumentsIntention.isApplicableTo(it, bindingContext)) {
                typeArgsToAdd.add(it to InsertExplicitTypeArgumentsIntention.createTypeArguments(it, bindingContext)!!)
            }
        }

        if (typeArgsToAdd.isEmpty()) return bindingContext

        for ((callExpr, typeArgs) in typeArgsToAdd) {
            callExpr.addAfter(typeArgs, callExpr.calleeExpression)
        }

        // reanalyze expression - new usages of type parameters may be added
        return analyze()
    }

    private fun insertExplicitReceivers(codeToInline: MutableCodeToInline, bindingContext: BindingContext) {
        val receiversToAdd = ArrayList<Pair<KtExpression, KtExpression>>()

        codeToInline.forEachDescendantOfType<KtSimpleNameExpression> { expression ->
            val target = bindingContext[BindingContext.REFERENCE_TARGET, expression] ?: return@forEachDescendantOfType

            //TODO: other types of references ('[]' etc)
            if (expression.mainReference.canBeResolvedViaImport(target, bindingContext)) {
                codeToInline.fqNamesToImport.add(target.importableFqName!!)
            }

            if (expression.getReceiverExpression() == null) {
                if (target is ValueParameterDescriptor && target.containingDeclaration == targetCallable) {
                    expression.putCopyableUserData(CodeToInline.PARAMETER_USAGE_KEY, target.name)
                }
                else if (target is TypeParameterDescriptor && target.containingDeclaration == targetCallable) {
                    expression.putCopyableUserData(CodeToInline.TYPE_PARAMETER_USAGE_KEY, target.name)
                }

                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null && resolvedCall.isReallySuccess()) {
                    val receiver = if (resolvedCall.resultingDescriptor.isExtension)
                        resolvedCall.extensionReceiver
                    else
                        resolvedCall.dispatchReceiver
                    if (receiver is ImplicitReceiver) {
                        val resolutionScope = expression.getResolutionScope(bindingContext, resolutionFacade)
                        val receiverExpression = receiver.asExpression(resolutionScope, psiFactory)
                        if (receiverExpression != null) {
                            receiversToAdd.add(expression to receiverExpression)
                        }
                    }
                }
            }
        }

        // add receivers in reverse order because arguments of a call were processed after the callee's name
        for ((expr, receiverExpression) in receiversToAdd.asReversed()) {
            val expressionToReplace = expr.parent as? KtCallExpression ?: expr
            codeToInline.replaceExpression(expressionToReplace,
                                           psiFactory.createExpressionByPattern("$0.$1", receiverExpression, expressionToReplace))
        }
    }
}