/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.light.LightMemberReference
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.findUsages.handlers.SliceUsageProcessor
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReadWriteAccessDetector
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi

class OutflowSlicer(
    element: KtElement,
    processor: SliceUsageProcessor,
    parentUsage: KotlinSliceUsage
) : Slicer(element, processor, parentUsage) {

    override fun processChildren(forcedExpressionMode: Boolean) {
        if (forcedExpressionMode) {
            (element as? KtExpression)?.let { processExpression(it) }
            return
        }

        when (element) {
            is KtProperty -> processVariable(element)

            is KtParameter -> processVariable(element)

            is KtFunction -> processFunction(element)

            is KtPropertyAccessor -> {
                if (element.isGetter) {
                    processVariable(element.property)
                }
            }

            is KtTypeReference -> {
                val declaration = element.parent
                require(declaration is KtCallableDeclaration)
                require(element == declaration.receiverTypeReference)

                if (declaration.isExpectDeclaration()) {
                    declaration.resolveToDescriptorIfAny(BodyResolveMode.FULL)
                        ?.actualsForExpected()
                        ?.forEach {
                            val actualDeclaration = (it as? DeclarationDescriptorWithSource)?.originalSource?.getPsi()
                            (actualDeclaration as? KtCallableDeclaration)?.receiverTypeReference?.passToProcessor()
                        }
                }

                when (declaration) {
                    is KtFunction -> {
                        processExtensionReceiver(declaration, declaration)
                    }
                    
                    is KtProperty -> {
                        //TODO: process only one of them or both depending on the usage type
                        declaration.getter?.let { processExtensionReceiver(declaration, it) }
                        declaration.setter?.let { processExtensionReceiver(declaration, it) }
                    }
                }
            }

            is KtExpression -> processExpression(element)
        }
    }

    private fun processVariable(variable: KtCallableDeclaration) {
        val withDereferences = parentUsage.params.showInstanceDereferences
        val accessKind = if (withDereferences) AccessKind.READ_OR_WRITE else AccessKind.READ_ONLY

        fun processVariableAccess(usageInfo: UsageInfo) {
            val refElement = usageInfo.element ?: return
            if (refElement !is KtExpression) {
                refElement.passToProcessor()
                return
            }

            val refExpression = KtPsiUtil.safeDeparenthesize(refElement)
            if (withDereferences) {
                refExpression.processDereferences()
            }
            if (!withDereferences || KotlinReadWriteAccessDetector.INSTANCE.getExpressionAccess(refExpression) == Access.Read) {
                refExpression.passToProcessor()
            }
        }

        if (variable is KtParameter) {
            if (!canProcessParameter(variable)) return //TODO

            val callable = variable.ownerFunction as? KtCallableDeclaration

            if (callable != null) {
                if (callable.isExpectDeclaration()) {
                    variable.resolveToDescriptorIfAny(BodyResolveMode.FULL)
                        ?.actualsForExpected()
                        ?.forEach {
                            (it as? DeclarationDescriptorWithSource)?.originalSource?.getPsi()?.passToProcessor()
                        }
                }

                val parameterIndex = variable.parameterIndex()
                callable.forEachOverridingElement(scope = analysisScope) { _, overridingMember ->
                    when (overridingMember) {
                        is KtCallableDeclaration -> {
                            val parameters = overridingMember.valueParameters
                            check(parameters.size == callable.valueParameters.size)
                            parameters[parameterIndex].passToProcessor()
                        }

                        is PsiMethod -> {
                            val parameters = overridingMember.parameterList.parameters
                            val shift = if (callable.receiverTypeReference != null) 1 else 0
                            check(parameters.size == callable.valueParameters.size + shift)
                            parameters[parameterIndex + shift].passToProcessor()
                        }

                        else -> {
                            // not supported
                        }
                    }
                    true
                }
            }
        }

        processVariableAccesses(variable, analysisScope, accessKind, ::processVariableAccess)
    }

    private fun processFunction(function: KtFunction) {
        //TODO: CallSliceProducer in all cases
        if (function is KtConstructor<*> || function is KtNamedFunction && function.name != null) {
            processCalls(function, includeOverriders = false, sliceProducer = CallSliceProducer)
        } else {
            processCalls(function, false, SliceProducer.Trivial)
        }
    }

    private fun processExtensionReceiver(declaration: KtCallableDeclaration, declarationWithBody: KtDeclarationWithBody) {
        //TODO: overriders
        //TODO: implicit receivers
        val resolutionFacade = declaration.getResolutionFacade()
        val callableDescriptor = declaration.resolveToDescriptorIfAny(resolutionFacade) as? CallableDescriptor ?: return
        val extensionReceiver = callableDescriptor.extensionReceiverParameter ?: return
        declarationWithBody.bodyExpression?.forEachDescendantOfType<KtThisExpression> { thisExpression ->
            val receiverDescriptor = thisExpression.resolveToCall(resolutionFacade)?.resultingDescriptor
            if (receiverDescriptor == extensionReceiver) {
                thisExpression.passToProcessor()
            }
        }
    }

    private fun processExpression(expression: KtExpression) {
        val expressionWithValue = when (expression) {
            is KtFunctionLiteral -> expression.parent as KtLambdaExpression
            else -> expression
        }
        expressionWithValue.processPseudocodeUsages { pseudoValue, instruction ->
            when (instruction) {
                is WriteValueInstruction -> {
                    if (!processIfReceiverValue(instruction, pseudoValue)) {
                        instruction.target.accessedDescriptor?.originalSource?.getPsi()?.passToProcessor()
                    }
                }

                is ReadValueInstruction -> {
                    processIfReceiverValue(instruction, pseudoValue)
                }

                is CallInstruction -> {
                    if (!processIfReceiverValue(instruction, pseudoValue)) {
                        instruction.arguments[pseudoValue]?.originalSource?.getPsi()?.passToProcessor()
                    }
                }

                is ReturnValueInstruction -> {
                    instruction.subroutine.passToProcessor()
                }

                is MagicInstruction -> {
                    when (instruction.kind) {
                        MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> instruction.outputValue.element?.passToProcessor()
                        else -> {
                        }
                    }
                }
            }
        }
    }

    private fun processIfReceiverValue(instruction: KtElementInstruction, pseudoValue: PseudoValue): Boolean {
        val receiverValue = (instruction as? InstructionWithReceivers)?.receiverValues?.get(pseudoValue) ?: return false
        val resolvedCall = instruction.element.resolveToCall() ?: return true
        when (resolvedCall.call.callType) {
            Call.CallType.DEFAULT -> {
                if (receiverValue == resolvedCall.extensionReceiver) {
                    val targetDeclaration = resolvedCall.resultingDescriptor.originalSource.getPsi()
                    (targetDeclaration as? KtCallableDeclaration)?.receiverTypeReference?.passToProcessor()
                }
            }

            Call.CallType.INVOKE -> {
                if (receiverValue == resolvedCall.dispatchReceiver && behaviour is LambdaCallsBehaviour) {
                    instruction.element.passToProcessor(behaviour)
                }
            }

            else -> {
                //TODO
            }
        }
        return true
    }

    private fun processDereferenceIfNeeded(
        expression: KtExpression,
        pseudoValue: PseudoValue,
        instr: InstructionWithReceivers
    ) {
        if (!parentUsage.params.showInstanceDereferences) return

        val receiver = instr.receiverValues[pseudoValue]
        val resolvedCall = when (instr) {
            is CallInstruction -> instr.resolvedCall
            is ReadValueInstruction -> (instr.target as? AccessTarget.Call)?.resolvedCall
            else -> null
        } ?: return

        if (receiver != null && resolvedCall.dispatchReceiver == receiver) {
            processor.process(KotlinSliceDereferenceUsage(expression, parentUsage, behaviour))
        }
    }

    private fun KtExpression.processPseudocodeUsages(processor: (PseudoValue, Instruction) -> Unit) {
        val pseudocode = pseudocodeCache[this] ?: return
        val pseudoValue = pseudocode.getElementValue(this) ?: return
        pseudocode.getUsages(pseudoValue).forEach { processor(pseudoValue, it) }
    }

    private fun KtExpression.processDereferences() {
        processPseudocodeUsages { pseudoValue, instr ->
            when (instr) {
                is ReadValueInstruction -> processDereferenceIfNeeded(this, pseudoValue, instr)
                is CallInstruction -> processDereferenceIfNeeded(this, pseudoValue, instr)
            }
        }
    }

    private object CallSliceProducer : SliceProducer {
        override fun produce(
            usage: UsageInfo,
            behaviour: KotlinSliceUsage.SpecialBehaviour?,
            parent: SliceUsage
        ): Collection<SliceUsage>? {
            when (val refElement = usage.element) {
                null -> {
                    val element = (usage.reference as? LightMemberReference)?.element ?: return emptyList()
                    return listOf(KotlinSliceUsage(element, parent, behaviour, false))
                }

                is KtExpression -> {
                    return mutableListOf<SliceUsage>().apply {
                        refElement.getCallElementForExactCallee()
                            ?.let { this += KotlinSliceUsage(it, parent, behaviour, false) }
                        refElement.getCallableReferenceForExactCallee()
                            ?.let { this += KotlinSliceUsage(it, parent, LambdaCallsBehaviour(SliceProducer.Trivial, behaviour), false) }
                    }
                }

                else -> {
                    return null // unknown type of usage - return null to process it "as is"
                }
            }
        }

        override fun equals(other: Any?) = other === this
        override fun hashCode() = 0

        private fun PsiElement.getCallElementForExactCallee(): PsiElement? {
            if (this is KtArrayAccessExpression) return this

            val operationRefExpr = getNonStrictParentOfType<KtOperationReferenceExpression>()
            if (operationRefExpr != null) return operationRefExpr.parent as? KtOperationExpression

            val parentCall = getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return null
            val callee = parentCall.calleeExpression?.let { KtPsiUtil.safeDeparenthesize(it) }
            if (callee == this || callee is KtConstructorCalleeExpression && callee.isAncestor(this, strict = true)) return parentCall

            return null
        }

        private fun PsiElement.getCallableReferenceForExactCallee(): KtCallableReferenceExpression? {
            val callableRef = getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } ?: return null
            val callee = KtPsiUtil.safeDeparenthesize(callableRef.callableReference)
            return if (callee == this) callableRef else null
        }
    }
}