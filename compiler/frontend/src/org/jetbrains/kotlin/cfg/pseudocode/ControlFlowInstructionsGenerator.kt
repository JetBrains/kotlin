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

package org.jetbrains.kotlin.cfg.pseudocode

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.*
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

import java.util.*

class ControlFlowInstructionsGenerator : ControlFlowBuilderAdapter() {
    private var builder: ControlFlowBuilder? = null

    override val delegateBuilder: ControlFlowBuilder
        get() = builder ?: throw AssertionError("Builder stack is empty in ControlFlowInstructionsGenerator!")

    private val loopInfo = Stack<LoopInfo>()
    private val blockScopes = Stack<BlockScope>()
    private val elementToLoopInfo = HashMap<KtLoopExpression, LoopInfo>()
    private val elementToSubroutineInfo = HashMap<KtElement, SubroutineInfo>()
    private var labelCount = 0

    private val builders = Stack<ControlFlowInstructionsGeneratorWorker>()

    private val allBlocks = Stack<BlockInfo>()

    private fun pushBuilder(scopingElement: KtElement, subroutine: KtElement) {
        val worker = ControlFlowInstructionsGeneratorWorker(scopingElement, subroutine)
        builders.push(worker)
        builder = worker
    }

    private fun popBuilder(): ControlFlowInstructionsGeneratorWorker {
        val worker = builders.pop()
        if (!builders.isEmpty()) {
            builder = builders.peek()
        }
        else {
            builder = null
        }
        return worker
    }

    override fun enterSubroutine(subroutine: KtElement) {
        val builder = builder
        if (builder != null && subroutine is KtFunctionLiteral) {
            pushBuilder(subroutine, builder.returnSubroutine)
        }
        else {
            pushBuilder(subroutine, subroutine)
        }
        delegateBuilder.enterBlockScope(subroutine)
        delegateBuilder.enterSubroutine(subroutine)
    }

    override fun exitSubroutine(subroutine: KtElement): Pseudocode {
        super.exitSubroutine(subroutine)
        delegateBuilder.exitBlockScope(subroutine)
        val worker = popBuilder()
        if (!builders.empty()) {
            val builder = builders.peek()
            builder.declareFunction(subroutine, worker.pseudocode)
        }
        return worker.pseudocode
    }

    private inner class ControlFlowInstructionsGeneratorWorker(scopingElement: KtElement, override val returnSubroutine: KtElement) : ControlFlowBuilder {

        val pseudocode: PseudocodeImpl
        private val error: Label
        private val sink: Label

        private val valueFactory = object : PseudoValueFactoryImpl() {
            override fun newValue(element: KtElement?, instruction: InstructionWithValue?): PseudoValue {
                val value = super.newValue(element, instruction)
                if (element != null) {
                    bindValue(value, element)
                }
                return value
            }
        }

        init {
            this.pseudocode = PseudocodeImpl(scopingElement)
            this.error = pseudocode.createLabel("error", null)
            this.sink = pseudocode.createLabel("sink", null)
        }

        private fun add(instruction: Instruction) {
            pseudocode.addInstruction(instruction)
        }

        override fun createUnboundLabel(): Label {
            return pseudocode.createLabel("L" + labelCount++, null)
        }

        override fun createUnboundLabel(name: String): Label {
            return pseudocode.createLabel("L" + labelCount++, name)
        }

        override fun enterLoop(expression: KtLoopExpression): LoopInfo {
            val info = LoopInfo(
                    expression,
                    createUnboundLabel("loop entry point"),
                    createUnboundLabel("loop exit point"),
                    createUnboundLabel("body entry point"),
                    createUnboundLabel("body exit point"),
                    createUnboundLabel("condition entry point"))
            bindLabel(info.entryPoint)
            elementToLoopInfo.put(expression, info)
            return info
        }

        override fun enterLoopBody(expression: KtLoopExpression) {
            val info = elementToLoopInfo[expression]!!
            bindLabel(info.bodyEntryPoint)
            loopInfo.push(info)
            allBlocks.push(info)
        }

        override fun exitLoopBody(expression: KtLoopExpression) {
            val info = loopInfo.pop()
            elementToLoopInfo.remove(expression)
            allBlocks.pop()
            bindLabel(info.bodyExitPoint)
        }

        override val currentLoop: KtLoopExpression?
            get() = if (loopInfo.empty()) null else loopInfo.peek().element

        override fun enterSubroutine(subroutine: KtElement) {
            val blockInfo = SubroutineInfo(
                    subroutine,
                    /* entry point */ createUnboundLabel(),
                    /* exit point  */ createUnboundLabel())
            elementToSubroutineInfo.put(subroutine, blockInfo)
            allBlocks.push(blockInfo)
            bindLabel(blockInfo.entryPoint)
            add(SubroutineEnterInstruction(subroutine, currentScope))
        }

        override val currentSubroutine: KtElement
            get() = pseudocode.correspondingElement

        override fun getLoopConditionEntryPoint(loop: KtLoopExpression): Label? {
            return elementToLoopInfo[loop]?.conditionEntryPoint
        }

        override fun getLoopExitPoint(loop: KtLoopExpression): Label? {
            // It's quite possible to have null here, see testBreakInsideLocal
            return elementToLoopInfo[loop]?.exitPoint
        }

        override fun getSubroutineExitPoint(labelElement: KtElement): Label? {
            // It's quite possible to have null here, e.g. for non-local returns (see KT-10823)
            return elementToSubroutineInfo[labelElement]?.exitPoint
        }

        private val currentScope: BlockScope
            get() = blockScopes.peek()

        override fun enterBlockScope(block: KtElement) {
            val current = if (blockScopes.isEmpty()) null else currentScope
            val scope = BlockScope(current, block)
            blockScopes.push(scope)
        }

        override fun exitBlockScope(block: KtElement) {
            val currentScope = currentScope
            assert(currentScope.block === block) {
                "Exit from not the current block scope.\n" +
                "Current scope is for a block: " + currentScope.block.text + ".\n" +
                "Exit from the scope for: " + block.text
            }
            blockScopes.pop()
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private fun handleJumpInsideTryFinally(jumpTarget: Label) {
            val finallyBlocks = ArrayList<TryFinallyBlockInfo>()

            for (blockInfo in allBlocks.asReversed()) {
                when (blockInfo) {
                    is BreakableBlockInfo -> if (blockInfo.referablePoints.contains(jumpTarget) || jumpTarget === error) {
                        for (finallyBlockInfo in finallyBlocks) {
                            finallyBlockInfo.generateFinallyBlock()
                        }
                        return
                    }
                    is TryFinallyBlockInfo -> finallyBlocks.add(blockInfo)
                }
            }
        }

        override fun exitSubroutine(subroutine: KtElement): Pseudocode {
            getSubroutineExitPoint(subroutine)?.let { bindLabel(it) }
            pseudocode.addExitInstruction(SubroutineExitInstruction(subroutine, currentScope, false))
            bindLabel(error)
            pseudocode.addErrorInstruction(SubroutineExitInstruction(subroutine, currentScope, true))
            bindLabel(sink)
            pseudocode.addSinkInstruction(SubroutineSinkInstruction(subroutine, currentScope, "<SINK>"))
            elementToSubroutineInfo.remove(subroutine)
            allBlocks.pop()
            return pseudocode
        }

        override fun mark(element: KtElement) {
            add(MarkInstruction(element, currentScope))
        }

        override fun getBoundValue(element: KtElement?): PseudoValue? {
            return pseudocode.getElementValue(element)
        }

        override fun bindValue(value: PseudoValue, element: KtElement) {
            pseudocode.bindElementToValue(element, value)
        }

        override fun newValue(element: KtElement?): PseudoValue {
            return valueFactory.newValue(element, null)
        }

        override fun returnValue(returnExpression: KtExpression, returnValue: PseudoValue, subroutine: KtElement) {
            val exitPoint = getSubroutineExitPoint(subroutine) ?: return
            handleJumpInsideTryFinally(exitPoint)
            add(ReturnValueInstruction(returnExpression, currentScope, exitPoint, returnValue, subroutine))
        }

        override fun returnNoValue(returnExpression: KtReturnExpression, subroutine: KtElement) {
            val exitPoint = getSubroutineExitPoint(subroutine) ?: return
            handleJumpInsideTryFinally(exitPoint)
            add(ReturnNoValueInstruction(returnExpression, currentScope, exitPoint, subroutine))
        }

        override fun write(
                assignment: KtElement,
                lValue: KtElement,
                rValue: PseudoValue,
                target: AccessTarget,
                receiverValues: Map<PseudoValue, ReceiverValue>) {
            add(WriteValueInstruction(assignment, currentScope, target, receiverValues, lValue, rValue))
        }

        override fun declareParameter(parameter: KtParameter) {
            add(VariableDeclarationInstruction(parameter, currentScope))
        }

        override fun declareVariable(property: KtVariableDeclaration) {
            add(VariableDeclarationInstruction(property, currentScope))
        }

        override fun declareFunction(subroutine: KtElement, pseudocode: Pseudocode) {
            add(LocalFunctionDeclarationInstruction(subroutine, pseudocode, currentScope))
        }

        override fun declareEntryOrObject(entryOrObject: KtClassOrObject) {
            add(VariableDeclarationInstruction(entryOrObject, currentScope))
        }

        override fun loadUnit(expression: KtExpression) {
            add(LoadUnitValueInstruction(expression, currentScope))
        }

        override fun jump(label: Label, element: KtElement) {
            handleJumpInsideTryFinally(label)
            add(UnconditionalJumpInstruction(element, label, currentScope))
        }

        override fun jumpOnFalse(label: Label, element: KtElement, conditionValue: PseudoValue?) {
            handleJumpInsideTryFinally(label)
            add(ConditionalJumpInstruction(element, false, currentScope, label, conditionValue))
        }

        override fun jumpOnTrue(label: Label, element: KtElement, conditionValue: PseudoValue?) {
            handleJumpInsideTryFinally(label)
            add(ConditionalJumpInstruction(element, true, currentScope, label, conditionValue))
        }

        override fun bindLabel(label: Label) {
            pseudocode.bindLabel(label as PseudocodeLabel)
        }

        override fun nondeterministicJump(label: Label, element: KtElement, inputValue: PseudoValue?) {
            handleJumpInsideTryFinally(label)
            add(NondeterministicJumpInstruction(element, listOf(label), currentScope, inputValue))
        }

        override fun nondeterministicJump(label: List<Label>, element: KtElement) {
            //todo
            //handleJumpInsideTryFinally(label);
            add(NondeterministicJumpInstruction(element, label, currentScope, null))
        }

        override fun jumpToError(element: KtElement) {
            handleJumpInsideTryFinally(error)
            add(UnconditionalJumpInstruction(element, error, currentScope))
        }

        override fun enterTryFinally(trigger: GenerationTrigger) {
            allBlocks.push(TryFinallyBlockInfo(trigger))
        }

        override fun throwException(throwExpression: KtThrowExpression, thrownValue: PseudoValue) {
            handleJumpInsideTryFinally(error)
            add(ThrowExceptionInstruction(throwExpression, currentScope, error, thrownValue))
        }

        override fun exitTryFinally() {
            val pop = allBlocks.pop()
            assert(pop is TryFinallyBlockInfo)
        }

        override fun repeatPseudocode(startLabel: Label, finishLabel: Label) {
            labelCount = pseudocode.repeatPart(startLabel, finishLabel, labelCount)
        }

        override fun loadConstant(expression: KtExpression, constant: CompileTimeConstant<*>?) = read(expression)

        override fun createAnonymousObject(expression: KtObjectLiteralExpression) = read(expression)

        override fun createLambda(expression: KtFunction) =
                read(if (expression is KtFunctionLiteral) expression.getParent() as KtLambdaExpression else expression)

        override fun loadStringTemplate(
                expression: KtStringTemplateExpression,
                inputValues: List<PseudoValue>
        ): InstructionWithValue =
                if (inputValues.isEmpty()) read(expression)
                else magic(expression, expression, inputValues, MagicKind.STRING_TEMPLATE)

        override fun magic(
                instructionElement: KtElement,
                valueElement: KtElement?,
                inputValues: List<PseudoValue>,
                kind: MagicKind): MagicInstruction {
            val instruction = MagicInstruction(
                    instructionElement, valueElement, currentScope, inputValues, kind, valueFactory)
            add(instruction)
            return instruction
        }

        override fun merge(expression: KtExpression, inputValues: List<PseudoValue>): MergeInstruction {
            val instruction = MergeInstruction(expression, currentScope, inputValues, valueFactory)
            add(instruction)
            return instruction
        }

        override fun readVariable(
                expression: KtExpression,
                resolvedCall: ResolvedCall<*>,
                receiverValues: Map<PseudoValue, ReceiverValue>
        ) = read(expression, resolvedCall, receiverValues)

        override fun call(
                valueElement: KtElement,
                resolvedCall: ResolvedCall<*>,
                receiverValues: Map<PseudoValue, ReceiverValue>,
                arguments: Map<PseudoValue, ValueParameterDescriptor>): CallInstruction {
            val returnType = resolvedCall.resultingDescriptor.returnType
            val instruction = CallInstruction(
                    valueElement,
                    currentScope,
                    resolvedCall,
                    receiverValues,
                    arguments,
                    if (returnType != null && KotlinBuiltIns.isNothing(returnType)) null else valueFactory)
            add(instruction)
            return instruction
        }

        override fun predefinedOperation(
                expression: KtExpression,
                operation: ControlFlowBuilder.PredefinedOperation,
                inputValues: List<PseudoValue>): OperationInstruction {
            return magic(expression, expression, inputValues, getMagicKind(operation))
        }

        private fun getMagicKind(operation: ControlFlowBuilder.PredefinedOperation): MagicKind {
            when (operation) {
                ControlFlowBuilder.PredefinedOperation.AND -> return MagicKind.AND
                ControlFlowBuilder.PredefinedOperation.OR -> return MagicKind.OR
                ControlFlowBuilder.PredefinedOperation.NOT_NULL_ASSERTION -> return MagicKind.NOT_NULL_ASSERTION
                else -> throw IllegalArgumentException("Invalid operation: " + operation)
            }
        }

        override fun read(
                element: KtElement,
                target: AccessTarget,
                receiverValues: Map<PseudoValue, ReceiverValue>
        ) = ReadValueInstruction(element, currentScope, target, receiverValues, valueFactory).apply {
            add(this)
        }

        private fun read(
                expression: KtExpression,
                resolvedCall: ResolvedCall<*>? = null,
                receiverValues: Map<PseudoValue, ReceiverValue> = emptyMap<PseudoValue, ReceiverValue>()
        ) = read(expression, if (resolvedCall != null) AccessTarget.Call(resolvedCall) else AccessTarget.BlackBox, receiverValues)
    }

    private class TryFinallyBlockInfo(private val finallyBlock: GenerationTrigger) : BlockInfo() {

        fun generateFinallyBlock() {
            finallyBlock.generate()
        }
    }

}
