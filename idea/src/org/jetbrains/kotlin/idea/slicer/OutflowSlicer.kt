/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access
import com.intellij.psi.PsiMethod
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.findUsages.handlers.SliceUsageProcessor
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReadWriteAccessDetector
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
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
                        processExtensionReceiverUsages(declaration, declaration.bodyExpression, mode)
                    }
                    
                    is KtProperty -> {
                        //TODO: process only one of them or both depending on the usage type
                        processExtensionReceiverUsages(declaration, declaration.getter?.bodyExpression, mode)
                        processExtensionReceiverUsages(declaration, declaration.setter?.bodyExpression, mode)
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
        processCalls(function, includeOverriders = false, CallSliceProducer)
    }

    private fun processExpression(expression: KtExpression) {
        val expressionWithValue = when (expression) {
            is KtFunctionLiteral -> expression.parent as KtLambdaExpression
            else -> expression
        }
        expressionWithValue.processPseudocodeUsages { pseudoValue, instruction ->
            when (instruction) {
                is WriteValueInstruction -> {
                    if (!pseudoValue.processIfReceiverValue(instruction, mode)) {
                        instruction.target.accessedDescriptor?.originalSource?.getPsi()?.passToProcessor()
                    }
                }

                is ReadValueInstruction -> {
                    pseudoValue.processIfReceiverValue(instruction, mode)
                }

                is CallInstruction -> {
                    if (!pseudoValue.processIfReceiverValue(instruction, mode)) {
                        val parameterDescriptor = instruction.arguments[pseudoValue] ?: return@processPseudocodeUsages
                        val parameter = parameterDescriptor.originalSource.getPsi()
                        if (parameter != null) {
                            parameter.passToProcessorInCallMode(instruction.element)
                        } else {
                            val function = parameterDescriptor.containingDeclaration as? FunctionDescriptor ?: return@processPseudocodeUsages
                            if (function.isImplicitInvokeFunction()) {
                                val receiverPseudoValue = instruction.receiverValues.entries.singleOrNull()?.key
                                    ?: return@processPseudocodeUsages
                                when (val createdAt = receiverPseudoValue.createdAt) {
                                    is ReadValueInstruction -> {
                                        val accessedDescriptor = createdAt.target.accessedDescriptor ?: return@processPseudocodeUsages
                                        if (accessedDescriptor is ValueParameterDescriptor) {
                                            val accessedDeclaration = accessedDescriptor.originalSource.getPsi() ?: return@processPseudocodeUsages
                                            val isExtension = accessedDescriptor.type.isExtensionFunctionType
                                            val shift = if (isExtension) 1 else 0
                                            val argumentIndex = parameterDescriptor.index - shift
                                            val newMode = if (argumentIndex >= 0)
                                                mode.withBehaviour(LambdaArgumentInflowBehaviour(argumentIndex))
                                            else
                                                mode.withBehaviour(LambdaReceiverInflowBehaviour)
                                            accessedDeclaration.passToProcessor(newMode)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is ReturnValueInstruction -> {
                    val subroutine = instruction.subroutine
                    if (subroutine is KtNamedFunction) {
                        val (newMode, callElement) = mode.popInlineFunctionCall(subroutine)
                        if (newMode != null) {
                            callElement?.passToProcessor(newMode)
                            return@processPseudocodeUsages
                        }
                    }

                    subroutine.passToProcessor()
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
            processor.process(KotlinSliceDereferenceUsage(expression, parentUsage, mode))
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
}
