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

abstract class ControlFlowBuilderAdapter : ControlFlowBuilder {

    protected abstract val delegateBuilder: ControlFlowBuilder

    override fun loadUnit(expression: KtExpression) {
        delegateBuilder.loadUnit(expression)
    }

    override fun loadConstant(expression: KtExpression, constant: CompileTimeConstant<*>?): InstructionWithValue {
        return delegateBuilder.loadConstant(expression, constant)
    }

    override fun createAnonymousObject(expression: KtObjectLiteralExpression): InstructionWithValue {
        return delegateBuilder.createAnonymousObject(expression)
    }

    override fun createLambda(expression: KtFunction): InstructionWithValue {
        return delegateBuilder.createLambda(expression)
    }

    override fun loadStringTemplate(expression: KtStringTemplateExpression, inputValues: List<PseudoValue>): InstructionWithValue {
        return delegateBuilder.loadStringTemplate(expression, inputValues)
    }

    override fun magic(
            instructionElement: KtElement,
            valueElement: KtElement?,
            inputValues: List<PseudoValue>,
            kind: MagicKind): MagicInstruction {
        return delegateBuilder.magic(instructionElement, valueElement, inputValues, kind)
    }

    override fun merge(expression: KtExpression, inputValues: List<PseudoValue>): MergeInstruction {
        return delegateBuilder.merge(expression, inputValues)
    }

    override fun readVariable(
            expression: KtExpression,
            resolvedCall: ResolvedCall<*>,
            receiverValues: Map<PseudoValue, ReceiverValue>): ReadValueInstruction {
        return delegateBuilder.readVariable(expression, resolvedCall, receiverValues)
    }

    override fun call(
            valueElement: KtElement,
            resolvedCall: ResolvedCall<*>,
            receiverValues: Map<PseudoValue, ReceiverValue>,
            arguments: Map<PseudoValue, ValueParameterDescriptor>): CallInstruction {
        return delegateBuilder.call(valueElement, resolvedCall, receiverValues, arguments)
    }

    override fun predefinedOperation(
            expression: KtExpression,
            operation: ControlFlowBuilder.PredefinedOperation,
            inputValues: List<PseudoValue>): OperationInstruction {
        return delegateBuilder.predefinedOperation(expression, operation, inputValues)
    }

    override fun createUnboundLabel(): Label {
        return delegateBuilder.createUnboundLabel()
    }

    override fun createUnboundLabel(name: String): Label {
        return delegateBuilder.createUnboundLabel(name)
    }

    override fun bindLabel(label: Label) {
        delegateBuilder.bindLabel(label)
    }

    override fun jump(label: Label, element: KtElement) {
        delegateBuilder.jump(label, element)
    }

    override fun jumpOnFalse(label: Label, element: KtElement, conditionValue: PseudoValue?) {
        delegateBuilder.jumpOnFalse(label, element, conditionValue)
    }

    override fun jumpOnTrue(label: Label, element: KtElement, conditionValue: PseudoValue?) {
        delegateBuilder.jumpOnTrue(label, element, conditionValue)
    }

    override fun nondeterministicJump(label: Label, element: KtElement, inputValue: PseudoValue?) {
        delegateBuilder.nondeterministicJump(label, element, inputValue)
    }

    override fun nondeterministicJump(label: List<Label>, element: KtElement) {
        delegateBuilder.nondeterministicJump(label, element)
    }

    override fun jumpToError(element: KtElement) {
        delegateBuilder.jumpToError(element)
    }

    override fun throwException(throwExpression: KtThrowExpression, thrownValue: PseudoValue) {
        delegateBuilder.throwException(throwExpression, thrownValue)
    }

    override fun getSubroutineExitPoint(labelElement: KtElement): Label? {
        return delegateBuilder.getSubroutineExitPoint(labelElement)
    }

    override fun getLoopConditionEntryPoint(loop: KtLoopExpression): Label? {
        return delegateBuilder.getLoopConditionEntryPoint(loop)
    }

    override fun getLoopExitPoint(loop: KtLoopExpression): Label? {
        return delegateBuilder.getLoopExitPoint(loop)
    }

    override fun enterLoop(expression: KtLoopExpression): LoopInfo {
        return delegateBuilder.enterLoop(expression)
    }

    override fun enterLoopBody(expression: KtLoopExpression) {
        delegateBuilder.enterLoopBody(expression)
    }

    override fun exitLoopBody(expression: KtLoopExpression) {
        delegateBuilder.exitLoopBody(expression)
    }

    override val currentLoop: KtLoopExpression?
        get() = delegateBuilder.currentLoop

    override fun enterTryFinally(trigger: GenerationTrigger) {
        delegateBuilder.enterTryFinally(trigger)
    }

    override fun exitTryFinally() {
        delegateBuilder.exitTryFinally()
    }

    override fun enterSubroutine(subroutine: KtElement) {
        delegateBuilder.enterSubroutine(subroutine)
    }

    override fun exitSubroutine(subroutine: KtElement): Pseudocode {
        return delegateBuilder.exitSubroutine(subroutine)
    }

    override val currentSubroutine: KtElement
        get() = delegateBuilder.currentSubroutine

    override val returnSubroutine: KtElement
        get() = delegateBuilder.returnSubroutine

    override fun returnValue(returnExpression: KtExpression, returnValue: PseudoValue, subroutine: KtElement) {
        delegateBuilder.returnValue(returnExpression, returnValue, subroutine)
    }

    override fun returnNoValue(returnExpression: KtReturnExpression, subroutine: KtElement) {
        delegateBuilder.returnNoValue(returnExpression, subroutine)
    }

    override fun read(element: KtElement, target: AccessTarget, receiverValues: Map<PseudoValue, ReceiverValue>) =
            delegateBuilder.read(element, target, receiverValues)

    override fun write(
            assignment: KtElement,
            lValue: KtElement,
            rValue: PseudoValue,
            target: AccessTarget,
            receiverValues: Map<PseudoValue, ReceiverValue>) {
        delegateBuilder.write(assignment, lValue, rValue, target, receiverValues)
    }

    override fun declareParameter(parameter: KtParameter) {
        delegateBuilder.declareParameter(parameter)
    }

    override fun declareVariable(property: KtVariableDeclaration) {
        delegateBuilder.declareVariable(property)
    }

    override fun declareFunction(subroutine: KtElement, pseudocode: Pseudocode) {
        delegateBuilder.declareFunction(subroutine, pseudocode)
    }

    override fun declareEntryOrObject(entryOrObject: KtClassOrObject) {
        delegateBuilder.declareEntryOrObject(entryOrObject)
    }

    override fun repeatPseudocode(startLabel: Label, finishLabel: Label) {
        delegateBuilder.repeatPseudocode(startLabel, finishLabel)
    }

    override fun mark(element: KtElement) {
        delegateBuilder.mark(element)
    }

    override fun getBoundValue(element: KtElement?): PseudoValue? {
        return delegateBuilder.getBoundValue(element)
    }

    override fun bindValue(value: PseudoValue, element: KtElement) {
        delegateBuilder.bindValue(value, element)
    }

    override fun newValue(element: KtElement?): PseudoValue {
        return delegateBuilder.newValue(element)
    }

    override fun enterBlockScope(block: KtElement) {
        delegateBuilder.enterBlockScope(block)
    }

    override fun exitBlockScope(block: KtElement) {
        delegateBuilder.exitBlockScope(block)
    }
}
