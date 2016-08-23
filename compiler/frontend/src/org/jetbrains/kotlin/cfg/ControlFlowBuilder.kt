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

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

interface ControlFlowBuilder {
    // Subroutines
    fun enterSubroutine(subroutine: KtElement)

    fun exitSubroutine(subroutine: KtElement): Pseudocode

    val currentSubroutine: KtElement
    val returnSubroutine: KtElement

    // Scopes
    fun enterBlockScope(block: KtElement)

    fun exitBlockScope(block: KtElement)

    fun getSubroutineExitPoint(labelElement: KtElement): Label?
    fun getLoopConditionEntryPoint(loop: KtLoopExpression): Label?
    fun getLoopExitPoint(loop: KtLoopExpression): Label?

    // Declarations
    fun declareParameter(parameter: KtParameter)

    fun declareVariable(property: KtVariableDeclaration)
    fun declareFunction(subroutine: KtElement, pseudocode: Pseudocode)

    fun declareEntryOrObject(entryOrObject: KtClassOrObject)

    // Labels
    fun createUnboundLabel(): Label

    fun createUnboundLabel(name: String): Label

    fun bindLabel(label: Label)

    // Jumps
    fun jump(label: Label, element: KtElement)

    fun jumpOnFalse(label: Label, element: KtElement, conditionValue: PseudoValue?)
    fun jumpOnTrue(label: Label, element: KtElement, conditionValue: PseudoValue?)
    fun nondeterministicJump(label: Label, element: KtElement, inputValue: PseudoValue?)  // Maybe, jump to label
    fun nondeterministicJump(label: List<Label>, element: KtElement)
    fun jumpToError(element: KtElement)

    fun returnValue(returnExpression: KtExpression, returnValue: PseudoValue, subroutine: KtElement)
    fun returnNoValue(returnExpression: KtReturnExpression, subroutine: KtElement)

    fun throwException(throwExpression: KtThrowExpression, thrownValue: PseudoValue)

    // Loops
    fun enterLoop(expression: KtLoopExpression): LoopInfo

    fun enterLoopBody(expression: KtLoopExpression)
    fun exitLoopBody(expression: KtLoopExpression)
    val currentLoop: KtLoopExpression?

    // Try-Finally
    fun enterTryFinally(trigger: GenerationTrigger)

    fun exitTryFinally()

    fun repeatPseudocode(startLabel: Label, finishLabel: Label)

    // Reading values
    fun mark(element: KtElement)

    fun getBoundValue(element: KtElement?): PseudoValue?
    fun bindValue(value: PseudoValue, element: KtElement)
    fun newValue(element: KtElement?): PseudoValue

    fun loadUnit(expression: KtExpression)

    fun loadConstant(expression: KtExpression, constant: CompileTimeConstant<*>?): InstructionWithValue
    fun createAnonymousObject(expression: KtObjectLiteralExpression): InstructionWithValue
    fun createLambda(expression: KtFunction): InstructionWithValue
    fun loadStringTemplate(expression: KtStringTemplateExpression, inputValues: List<PseudoValue>): InstructionWithValue

    fun magic(
            instructionElement: KtElement,
            valueElement: KtElement?,
            inputValues: List<PseudoValue>,
            kind: MagicKind): MagicInstruction

    fun merge(
            expression: KtExpression,
            inputValues: List<PseudoValue>): MergeInstruction

    fun readVariable(
            expression: KtExpression,
            resolvedCall: ResolvedCall<*>,
            receiverValues: Map<PseudoValue, ReceiverValue>): ReadValueInstruction

    fun call(
            valueElement: KtElement,
            resolvedCall: ResolvedCall<*>,
            receiverValues: Map<PseudoValue, ReceiverValue>,
            arguments: Map<PseudoValue, ValueParameterDescriptor>): CallInstruction

    enum class PredefinedOperation {
        AND,
        OR,
        NOT_NULL_ASSERTION
    }

    fun predefinedOperation(
            expression: KtExpression,
            operation: PredefinedOperation,
            inputValues: List<PseudoValue>): OperationInstruction

    fun read(element: KtElement, target: AccessTarget, receiverValues: Map<PseudoValue, ReceiverValue>): ReadValueInstruction

    fun write(
            assignment: KtElement,
            lValue: KtElement,
            rValue: PseudoValue,
            target: AccessTarget,
            receiverValues: Map<PseudoValue, ReceiverValue>)
}
