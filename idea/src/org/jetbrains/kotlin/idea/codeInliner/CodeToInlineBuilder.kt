/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInliner

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.asExpression
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.intentions.SpecifyExplicitLambdaSignatureIntention
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
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
        analyze: (KtExpression) -> BindingContext,
        reformat: Boolean,
    ): CodeToInline {
        val alwaysKeepMainExpression =
            when (val descriptor = mainExpression?.getResolvedCall(analyze(mainExpression))?.resultingDescriptor) {
                is PropertyDescriptor -> descriptor.getter?.isDefault == false
                else -> false
            }

        val codeToInline = MutableCodeToInline(
            mainExpression,
            statementsBefore.toMutableList(),
            mutableSetOf(),
            alwaysKeepMainExpression
        )

        insertExplicitTypeArguments(codeToInline, analyze)

        processReferences(codeToInline, analyze, reformat)

        if (mainExpression != null) {
            val functionLiteralExpression = mainExpression.unpackFunctionLiteral(true)
            if (functionLiteralExpression != null) {
                val functionLiteralParameterTypes = getParametersForFunctionLiteral(functionLiteralExpression, analyze)
                if (functionLiteralParameterTypes != null) {
                    codeToInline.addPostInsertionAction(mainExpression) { inlinedExpression ->
                        addFunctionLiteralParameterTypes(functionLiteralParameterTypes, inlinedExpression)
                    }
                }
            }
        }

        return codeToInline.toNonMutable()
    }

    private fun getParametersForFunctionLiteral(
        functionLiteralExpression: KtLambdaExpression,
        analyze: (KtExpression) -> BindingContext
    ): String? {
        val context = analyze(functionLiteralExpression)
        val lambdaDescriptor = context.get(BindingContext.FUNCTION, functionLiteralExpression.functionLiteral)
        if (lambdaDescriptor == null ||
            ErrorUtils.containsErrorTypeInParameters(lambdaDescriptor) ||
            ErrorUtils.containsErrorType(lambdaDescriptor.returnType)
        ) return null

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
            val hasCantInferParameter = factory == Errors.CANNOT_INFER_PARAMETER_TYPE && element.parent.parent == functionLiteral
            val hasUnresolvedItOrThis = factory == Errors.UNRESOLVED_REFERENCE &&
                    element.text == "it" &&
                    element.getStrictParentOfType<KtFunctionLiteral>() == functionLiteral

            hasCantInferParameter || hasUnresolvedItOrThis
        }
    }

    private fun insertExplicitTypeArguments(codeToInline: MutableCodeToInline, analyze: (KtExpression) -> BindingContext) {
        val typeArgsToAdd = ArrayList<Pair<KtCallExpression, KtTypeArgumentList>>()
        codeToInline.forEachDescendantOfType<KtCallExpression> {
            val expression = it.parent as? KtQualifiedExpression ?: it
            val bindingContext = analyze(expression)
            if (InsertExplicitTypeArgumentsIntention.isApplicableTo(it, bindingContext)) {
                typeArgsToAdd.add(it to InsertExplicitTypeArgumentsIntention.createTypeArguments(it, bindingContext)!!)
            }
        }

        if (typeArgsToAdd.isEmpty()) return
        for ((callExpr, typeArgs) in typeArgsToAdd) {
            callExpr.addAfter(typeArgs, callExpr.calleeExpression)
        }
    }

    private fun processReferences(codeToInline: MutableCodeToInline, analyze: (KtExpression) -> BindingContext, reformat: Boolean) {
        val receiversToAdd = ArrayList<Triple<KtExpression, KtExpression, KotlinType>>()
        val targetDispatchReceiverType = targetCallable.dispatchReceiverParameter?.value?.type
        val targetExtensionReceiverType = targetCallable.extensionReceiverParameter?.value?.type

        codeToInline.forEachDescendantOfType<KtSimpleNameExpression> { expression ->
            val bindingContext = analyze(expression)
            val target = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, expression]
                ?: bindingContext[BindingContext.REFERENCE_TARGET, expression]
                ?: return@forEachDescendantOfType

            //TODO: other types of references ('[]' etc)
            if (expression.canBeResolvedViaImport(target, bindingContext)) {
                val importableFqName = target.importableFqName

                if (importableFqName != null) {
                    val lexicalScope = (expression.containingFile as? KtFile)?.getResolutionScope(bindingContext, resolutionFacade)
                    val lookupName = lexicalScope?.findClassifier(importableFqName.shortName(), NoLookupLocation.FROM_IDE)
                        ?.typeConstructor
                        ?.declarationDescriptor
                        ?.fqNameOrNull()

                    codeToInline.fqNamesToImport.add(lookupName ?: importableFqName)
                }
            }

            if (expression.getReceiverExpression() == null) {
                (targetCallable.safeAs<ImportedFromObjectCallableDescriptor<*>>()?.callableFromObject ?: targetCallable).let {
                    if (target is ValueParameterDescriptor && target.containingDeclaration == it) {
                        expression.putCopyableUserData(CodeToInline.PARAMETER_USAGE_KEY, target.name)
                    } else if (target is TypeParameterDescriptor && target.containingDeclaration == it) {
                        expression.putCopyableUserData(CodeToInline.TYPE_PARAMETER_USAGE_KEY, target.name)
                    }
                }

                if (targetCallable !is ImportedFromObjectCallableDescriptor<*>) {
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
                                receiversToAdd.add(Triple(expression, receiverExpression, receiver.type))
                            }
                        }
                    }
                }
            }
        }

        // add receivers in reverse order because arguments of a call were processed after the callee's name
        for ((expr, receiverExpression, receiverType) in receiversToAdd.asReversed()) {
            val expressionToReplace = expr.parent as? KtCallExpression ?: expr
            val replaced = codeToInline.replaceExpression(
                expressionToReplace,
                psiFactory.createExpressionByPattern(
                    "$0.$1", receiverExpression, expressionToReplace,
                    reformat = reformat
                )
            ) as? KtQualifiedExpression
            if (receiverType != targetDispatchReceiverType && receiverType != targetExtensionReceiverType) {
                replaced?.receiverExpression?.putCopyableUserData(CodeToInline.SIDE_RECEIVER_USAGE_KEY, Unit)
            }
        }
    }
}