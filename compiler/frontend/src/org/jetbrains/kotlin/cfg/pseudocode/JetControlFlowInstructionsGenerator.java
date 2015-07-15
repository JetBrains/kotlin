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

package org.jetbrains.kotlin.cfg.pseudocode;

import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cfg.*;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.*;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.*;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.JetType;

import java.util.*;

public class JetControlFlowInstructionsGenerator extends JetControlFlowBuilderAdapter {
    private JetControlFlowBuilder builder = null;

    private final Stack<LoopInfo> loopInfo = new Stack<LoopInfo>();
    private final Stack<LexicalScope> lexicalScopes = new Stack<LexicalScope>();
    private final Map<JetElement, BreakableBlockInfo> elementToBlockInfo = new HashMap<JetElement, BreakableBlockInfo>();
    private int labelCount = 0;

    private final Stack<JetControlFlowInstructionsGeneratorWorker> builders = new Stack<JetControlFlowInstructionsGeneratorWorker>();

    private final Stack<BlockInfo> allBlocks = new Stack<BlockInfo>();

    @NotNull
    @Override
    protected JetControlFlowBuilder getDelegateBuilder() {
        return builder;
    }

    private void pushBuilder(JetElement scopingElement, JetElement subroutine) {
        JetControlFlowInstructionsGeneratorWorker worker = new JetControlFlowInstructionsGeneratorWorker(scopingElement, subroutine);
        builders.push(worker);
        builder = worker;
    }

    private JetControlFlowInstructionsGeneratorWorker popBuilder(@NotNull JetElement element) {
        JetControlFlowInstructionsGeneratorWorker worker = builders.pop();
        if (!builders.isEmpty()) {
            builder = builders.peek();
        }
        else {
            builder = null;
        }
        return worker;
    }

    @Override
    public void enterSubroutine(@NotNull JetElement subroutine) {
        if (builder != null && subroutine instanceof JetFunctionLiteral) {
            pushBuilder(subroutine, builder.getReturnSubroutine());
        }
        else {
            pushBuilder(subroutine, subroutine);
        }
        assert builder != null;
        builder.enterLexicalScope(subroutine);
        builder.enterSubroutine(subroutine);
    }

    @NotNull
    @Override
    public Pseudocode exitSubroutine(@NotNull JetElement subroutine) {
        super.exitSubroutine(subroutine);
        builder.exitLexicalScope(subroutine);
        JetControlFlowInstructionsGeneratorWorker worker = popBuilder(subroutine);
        if (!builders.empty()) {
            JetControlFlowInstructionsGeneratorWorker builder = builders.peek();
            builder.declareFunction(subroutine, worker.getPseudocode());
        }
        return worker.getPseudocode();
    }

    private class JetControlFlowInstructionsGeneratorWorker implements JetControlFlowBuilder {

        private final PseudocodeImpl pseudocode;
        private final Label error;
        private final Label sink;
        private final JetElement returnSubroutine;

        private final PseudoValueFactory valueFactory = new PseudoValueFactoryImpl() {
            @NotNull
            @Override
            public PseudoValue newValue(@Nullable JetElement element, @Nullable InstructionWithValue instruction) {
                PseudoValue value = super.newValue(element, instruction);
                if (element != null) {
                    bindValue(value, element);
                }
                return value;
            }
        };

        private JetControlFlowInstructionsGeneratorWorker(@NotNull JetElement scopingElement, @NotNull JetElement returnSubroutine) {
            this.pseudocode = new PseudocodeImpl(scopingElement);
            this.error = pseudocode.createLabel("error", null);
            this.sink = pseudocode.createLabel("sink", null);
            this.returnSubroutine = returnSubroutine;
        }

        public PseudocodeImpl getPseudocode() {
            return pseudocode;
        }

        private void add(@NotNull Instruction instruction) {
            pseudocode.addInstruction(instruction);
        }

        @NotNull
        @Override
        public final Label createUnboundLabel() {
            return pseudocode.createLabel("L" + labelCount++, null);
        }

        @NotNull
        @Override
        public Label createUnboundLabel(@NotNull String name) {
            return pseudocode.createLabel("L" + labelCount++, name);
        }

        @NotNull
        @Override
        public final LoopInfo enterLoop(@NotNull JetLoopExpression expression) {
            LoopInfo info = new LoopInfo(
                    expression,
                    createUnboundLabel("loop entry point"),
                    createUnboundLabel("loop exit point"),
                    createUnboundLabel("body entry point"),
                    createUnboundLabel("body exit point"),
                    createUnboundLabel("condition entry point"));
            bindLabel(info.getEntryPoint());
            elementToBlockInfo.put(expression, info);
            return info;
        }

        @Override
        public void enterLoopBody(@NotNull JetLoopExpression expression) {
            LoopInfo info = (LoopInfo) elementToBlockInfo.get(expression);
            bindLabel(info.getBodyEntryPoint());
            loopInfo.push(info);
            allBlocks.push(info);
        }

        @Override
        public final void exitLoopBody(@NotNull JetLoopExpression expression) {
            LoopInfo info = loopInfo.pop();
            elementToBlockInfo.remove(expression);
            allBlocks.pop();
            bindLabel(info.getBodyExitPoint());
        }

        @Override
        public JetLoopExpression getCurrentLoop() {
            return loopInfo.empty() ? null : loopInfo.peek().getElement();
        }

        @Override
        public void enterSubroutine(@NotNull JetElement subroutine) {
            BreakableBlockInfo blockInfo = new BreakableBlockInfo(
                    subroutine,
                    /* entry point */ createUnboundLabel(),
                    /* exit point  */ createUnboundLabel());
            elementToBlockInfo.put(subroutine, blockInfo);
            allBlocks.push(blockInfo);
            bindLabel(blockInfo.getEntryPoint());
            add(new SubroutineEnterInstruction(subroutine, getCurrentScope()));
        }

        @NotNull
        @Override
        public JetElement getCurrentSubroutine() {
            return pseudocode.getCorrespondingElement();
        }

        @Override
        public JetElement getReturnSubroutine() {
            return returnSubroutine;// subroutineInfo.empty() ? null : subroutineInfo.peek().getElement();
        }

        @NotNull
        @Override
        public Label getEntryPoint(@NotNull JetElement labelElement) {
            return elementToBlockInfo.get(labelElement).getEntryPoint();
        }

        @NotNull
        @Override
        public Label getConditionEntryPoint(@NotNull JetElement labelElement) {
            BreakableBlockInfo blockInfo = elementToBlockInfo.get(labelElement);
            assert blockInfo instanceof LoopInfo : "expected LoopInfo for " + labelElement.getText() ;
            return ((LoopInfo)blockInfo).getConditionEntryPoint();
        }

        @NotNull
        @Override
        public Label getExitPoint(@NotNull JetElement labelElement) {
            BreakableBlockInfo blockInfo = elementToBlockInfo.get(labelElement);
            assert blockInfo != null : labelElement.getText();
            return blockInfo.getExitPoint();
        }

        @NotNull
        private LexicalScope getCurrentScope() {
            return lexicalScopes.peek();
        }

        @Override
        public void enterLexicalScope(@NotNull JetElement element) {
            LexicalScope current = lexicalScopes.isEmpty() ? null : getCurrentScope();
            LexicalScope scope = new LexicalScope(current, element);
            lexicalScopes.push(scope);
        }

        @Override
        public void exitLexicalScope(@NotNull JetElement element) {
            LexicalScope currentScope = getCurrentScope();
            assert currentScope.getElement() == element : "Exit from not the current lexical scope.\n" +
                    "Current scope is for: " + currentScope.getElement() + ".\n" +
                    "Exit from the scope for: " + element.getText();
            lexicalScopes.pop();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private void handleJumpInsideTryFinally(Label jumpTarget) {
            List<TryFinallyBlockInfo> finallyBlocks = new ArrayList<TryFinallyBlockInfo>();

            for (int i = allBlocks.size() - 1; i >= 0; i--) {
                BlockInfo blockInfo = allBlocks.get(i);
                if (blockInfo instanceof BreakableBlockInfo) {
                    BreakableBlockInfo breakableBlockInfo = (BreakableBlockInfo) blockInfo;
                    if (breakableBlockInfo.getReferablePoints().contains(jumpTarget) || jumpTarget == error) {
                        for (int j = finallyBlocks.size() - 1; j >= 0; j--) {
                            finallyBlocks.get(j).generateFinallyBlock();
                        }
                        break;
                    }
                }
                else if (blockInfo instanceof TryFinallyBlockInfo) {
                    TryFinallyBlockInfo tryFinallyBlockInfo = (TryFinallyBlockInfo) blockInfo;
                    finallyBlocks.add(tryFinallyBlockInfo);
                }
            }
        }

        @NotNull
        @Override
        public Pseudocode exitSubroutine(@NotNull JetElement subroutine) {
            bindLabel(getExitPoint(subroutine));
            pseudocode.addExitInstruction(new SubroutineExitInstruction(subroutine, getCurrentScope(), false));
            bindLabel(error);
            pseudocode.addErrorInstruction(new SubroutineExitInstruction(subroutine, getCurrentScope(), true));
            bindLabel(sink);
            pseudocode.addSinkInstruction(new SubroutineSinkInstruction(subroutine, getCurrentScope(), "<SINK>"));
            elementToBlockInfo.remove(subroutine);
            allBlocks.pop();
            return pseudocode;
        }

        @Override
        public void mark(@NotNull JetElement element) {
            add(new MarkInstruction(element, getCurrentScope()));
        }

        @Nullable
        @Override
        public PseudoValue getBoundValue(@Nullable JetElement element) {
            return pseudocode.getElementValue(element);
        }

        @Override
        public void bindValue(@NotNull PseudoValue value, @NotNull JetElement element) {
            pseudocode.bindElementToValue(element, value);
        }

        @NotNull
        @Override
        public PseudoValue newValue(@Nullable JetElement element) {
            return valueFactory.newValue(element, null);
        }

        @Override
        public void returnValue(@NotNull JetExpression returnExpression, @NotNull PseudoValue returnValue, @NotNull JetElement subroutine) {
            Label exitPoint = getExitPoint(subroutine);
            handleJumpInsideTryFinally(exitPoint);
            add(new ReturnValueInstruction(returnExpression, getCurrentScope(), exitPoint, returnValue));
        }

        @Override
        public void returnNoValue(@NotNull JetReturnExpression returnExpression, @NotNull JetElement subroutine) {
            Label exitPoint = getExitPoint(subroutine);
            handleJumpInsideTryFinally(exitPoint);
            add(new ReturnNoValueInstruction(returnExpression, getCurrentScope(), exitPoint));
        }

        @Override
        public void write(
                @NotNull JetElement assignment,
                @NotNull JetElement lValue,
                @NotNull PseudoValue rValue,
                @NotNull AccessTarget target,
                @NotNull Map<PseudoValue, ReceiverValue> receiverValues) {
            add(new WriteValueInstruction(assignment, getCurrentScope(), target, receiverValues, lValue, rValue));
        }

        @Override
        public void declareParameter(@NotNull JetParameter parameter) {
            add(new VariableDeclarationInstruction(parameter, getCurrentScope()));
        }

        @Override
        public void declareVariable(@NotNull JetVariableDeclaration property) {
            add(new VariableDeclarationInstruction(property, getCurrentScope()));
        }

        @Override
        public void declareFunction(@NotNull JetElement subroutine, @NotNull Pseudocode pseudocode) {
            add(new LocalFunctionDeclarationInstruction(subroutine, pseudocode, getCurrentScope()));
        }

        @Override
        public void loadUnit(@NotNull JetExpression expression) {
            add(new LoadUnitValueInstruction(expression, getCurrentScope()));
        }

        @Override
        public void jump(@NotNull Label label, @NotNull JetElement element) {
            handleJumpInsideTryFinally(label);
            add(new UnconditionalJumpInstruction(element, label, getCurrentScope()));
        }

        @Override
        public void jumpOnFalse(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue conditionValue) {
            handleJumpInsideTryFinally(label);
            add(new ConditionalJumpInstruction(element, false, getCurrentScope(), label, conditionValue));
        }

        @Override
        public void jumpOnTrue(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue conditionValue) {
            handleJumpInsideTryFinally(label);
            add(new ConditionalJumpInstruction(element, true, getCurrentScope(), label, conditionValue));
        }

        @Override
        public void bindLabel(@NotNull Label label) {
            pseudocode.bindLabel(label);
        }

        @Override
        public void nondeterministicJump(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue inputValue) {
            handleJumpInsideTryFinally(label);
            add(new NondeterministicJumpInstruction(element, Collections.singletonList(label), getCurrentScope(), inputValue));
        }

        @Override
        public void nondeterministicJump(@NotNull List<Label> labels, @NotNull JetElement element) {
            //todo
            //handleJumpInsideTryFinally(label);
            add(new NondeterministicJumpInstruction(element, labels, getCurrentScope(), null));
        }

        @Override
        public void jumpToError(@NotNull JetElement element) {
            handleJumpInsideTryFinally(error);
            add(new UnconditionalJumpInstruction(element, error, getCurrentScope()));
        }

        @Override
        public void enterTryFinally(@NotNull GenerationTrigger generationTrigger) {
            allBlocks.push(new TryFinallyBlockInfo(generationTrigger));
        }

        @Override
        public void throwException(@NotNull JetThrowExpression expression, @NotNull PseudoValue thrownValue) {
            handleJumpInsideTryFinally(error);
            add(new ThrowExceptionInstruction(expression, getCurrentScope(), error, thrownValue));
        }

        @Override
        public void exitTryFinally() {
            BlockInfo pop = allBlocks.pop();
            assert pop instanceof TryFinallyBlockInfo;
        }

        @Override
        public void repeatPseudocode(@NotNull Label startLabel, @NotNull Label finishLabel) {
            labelCount = pseudocode.repeatPart(startLabel, finishLabel, labelCount);
        }

        @NotNull
        @Override
        public InstructionWithValue loadConstant(@NotNull JetExpression expression, @Nullable CompileTimeConstant<?> constant) {
            return read(expression);
        }

        @NotNull
        @Override
        public InstructionWithValue createAnonymousObject(@NotNull JetObjectLiteralExpression expression) {
            return read(expression);
        }

        @NotNull
        @Override
        public InstructionWithValue createLambda(@NotNull JetFunction expression) {
            return read(expression instanceof JetFunctionLiteral ? (JetFunctionLiteralExpression) expression.getParent() : expression);
        }

        @NotNull
        @Override
        public InstructionWithValue loadStringTemplate(@NotNull JetStringTemplateExpression expression, @NotNull List<PseudoValue> inputValues) {
            return inputValues.isEmpty() ? read(expression) : magic(expression, expression, inputValues, MagicKind.STRING_TEMPLATE);
        }

        @NotNull
        @Override
        public MagicInstruction magic(
                @NotNull JetElement instructionElement,
                @Nullable JetElement valueElement,
                @NotNull List<PseudoValue> inputValues,
                @NotNull MagicKind kind
        ) {
            MagicInstruction instruction = new MagicInstruction(
                    instructionElement, valueElement, getCurrentScope(), inputValues, kind, valueFactory
            );
            add(instruction);
            return instruction;
        }

        @NotNull
        @Override
        public MergeInstruction merge(@NotNull JetExpression expression, @NotNull List<PseudoValue> inputValues) {
            MergeInstruction instruction = new MergeInstruction(expression, getCurrentScope(), inputValues, valueFactory);
            add(instruction);
            return instruction;
        }

        @NotNull
        @Override
        public ReadValueInstruction readVariable(
                @NotNull JetExpression expression,
                @NotNull ResolvedCall<?> resolvedCall,
                @NotNull Map<PseudoValue, ReceiverValue> receiverValues
        ) {
            return read(expression, resolvedCall, receiverValues);
        }

        @NotNull
        @Override
        public CallInstruction call(
                @NotNull JetElement valueElement,
                @NotNull ResolvedCall<?> resolvedCall,
                @NotNull Map<PseudoValue, ReceiverValue> receiverValues,
                @NotNull Map<PseudoValue, ValueParameterDescriptor> arguments
        ) {
            JetType returnType = resolvedCall.getResultingDescriptor().getReturnType();
            CallInstruction instruction = new CallInstruction(
                    valueElement,
                    getCurrentScope(),
                    resolvedCall,
                    receiverValues,
                    arguments,
                    returnType != null && KotlinBuiltIns.isNothing(returnType) ? null : valueFactory
            );
            add(instruction);
            return instruction;
        }

        @NotNull
        @Override
        public OperationInstruction predefinedOperation(
                @NotNull JetExpression expression,
                @NotNull PredefinedOperation operation,
                @NotNull List<PseudoValue> inputValues
        ) {
            return magic(expression, expression, inputValues, getMagicKind(operation));
        }

        @NotNull
        private MagicKind getMagicKind(@NotNull PredefinedOperation operation) {
            switch(operation) {
                case AND:
                    return MagicKind.AND;
                case OR:
                    return MagicKind.OR;
                case NOT_NULL_ASSERTION:
                    return MagicKind.NOT_NULL_ASSERTION;
                default:
                    throw new IllegalArgumentException("Invalid operation: " + operation);
            }
        }

        @NotNull
        private ReadValueInstruction read(
                @NotNull JetExpression expression,
                @Nullable ResolvedCall<?> resolvedCall,
                @NotNull Map<PseudoValue, ReceiverValue> receiverValues
        ) {
            AccessTarget accessTarget = resolvedCall != null ? new AccessTarget.Call(resolvedCall) : AccessTarget.BlackBox.INSTANCE$;
            ReadValueInstruction instruction = new ReadValueInstruction(
                    expression, getCurrentScope(), accessTarget, receiverValues, valueFactory
            );
            add(instruction);
            return instruction;
        }

        @NotNull
        private ReadValueInstruction read(@NotNull JetExpression expression) {
            return read(expression, null, Collections.<PseudoValue, ReceiverValue>emptyMap());
        }
    }

    public static class TryFinallyBlockInfo extends BlockInfo {
        private final GenerationTrigger finallyBlock;

        private TryFinallyBlockInfo(GenerationTrigger finallyBlock) {
            this.finallyBlock = finallyBlock;
        }

        public void generateFinallyBlock() {
            finallyBlock.generate();
        }
    }

}
