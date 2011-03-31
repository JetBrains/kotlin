package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.JetControlFlowBuilder;
import org.jetbrains.jet.lang.cfg.JetControlFlowBuilderAdapter;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author abreslav
 */
public class JetControlFlowInstructionsGenerator extends JetControlFlowBuilderAdapter {

    private int labelCount = 0;
    private final Stack<JetControlFlowInstructionsGeneratorWorker> builders = new Stack<JetControlFlowInstructionsGeneratorWorker>();

    public JetControlFlowInstructionsGenerator() {
        super(null);
        pushBuilder();
    }

    private void pushBuilder() {
        Pseudocode parentPseudocode = builder == null ? new Pseudocode(null) : builders.peek().getPseudocode();
        JetControlFlowInstructionsGeneratorWorker worker = new JetControlFlowInstructionsGeneratorWorker(parentPseudocode);
        builders.push(worker);
        builder = worker;
    }

    private JetControlFlowInstructionsGeneratorWorker popBuilder() {
        JetControlFlowInstructionsGeneratorWorker worker = builders.pop();
        builder = builders.peek();
        return worker;
    }

    @Override
    public void enterSubroutine(@NotNull JetElement subroutine, boolean isFunctionLiteral) {
        if (isFunctionLiteral) {
            pushBuilder();
            builder.enterSubroutine(subroutine, false);
        }
        else {
            super.enterSubroutine(subroutine, isFunctionLiteral);
        }
    }

    @Override
    public void exitSubroutine(@NotNull JetElement subroutine, boolean functionLiteral) {
        super.exitSubroutine(subroutine, functionLiteral);
        if (functionLiteral) {
            JetControlFlowInstructionsGeneratorWorker worker = popBuilder();
            JetControlFlowInstructionsGeneratorWorker builder = builders.peek();
            FunctionLiteralValueInstruction instruction = new FunctionLiteralValueInstruction(subroutine);
            builder.add(instruction);
            instruction.setBody(worker.getPseudocode());
        }
    }

    public Pseudocode getPseudocode() {
        return builders.peek().getPseudocode();
    }

    private static class BlockInfo {
        private final JetElement element;
        private final Label entryPoint;
        private final Label exitPoint;

        private BlockInfo(JetElement element, Label entryPoint, Label exitPoint) {
            this.element = element;
            this.entryPoint = entryPoint;
            this.exitPoint = exitPoint;
        }

        public JetElement getElement() {
            return element;
        }

        public Label getEntryPoint() {
            return entryPoint;
        }

        public Label getExitPoint() {
            return exitPoint;
        }
    }

    private class JetControlFlowInstructionsGeneratorWorker implements JetControlFlowBuilder {
        private final Stack<BlockInfo> loopInfo = new Stack<BlockInfo>();
        private final Stack<BlockInfo> subroutineInfo = new Stack<BlockInfo>();

        private final Map<JetElement, BlockInfo> elementToBlockInfo = new HashMap<JetElement, BlockInfo>();

        private final Pseudocode pseudocode;

        private JetControlFlowInstructionsGeneratorWorker(@Nullable Pseudocode parent) {
            this.pseudocode = new Pseudocode(parent);
        }

        public Pseudocode getPseudocode() {
            return pseudocode;
        }

        private void add(Instruction instruction) {
            pseudocode.addInstruction(instruction);
        }

        @NotNull
        @Override
        public final Label createUnboundLabel() {
            return new Label("l" + labelCount++);
        }

        @Override
        public final Label enterLoop(@NotNull JetExpression expression, Label loopExitPoint) {
            Label label = createUnboundLabel();
            bindLabel(label);
            BlockInfo blockInfo = new BlockInfo(expression, label, loopExitPoint);
            loopInfo.push(blockInfo);
            elementToBlockInfo.put(expression, blockInfo);
            return label;
        }

        @Override
        public final void exitLoop(@NotNull JetExpression expression) {
            BlockInfo info = loopInfo.pop();
            elementToBlockInfo.remove(expression);
            bindLabel(info.getExitPoint());
        }

        @Override
        public JetElement getCurrentLoop() {
            return loopInfo.empty() ? null : loopInfo.peek().getElement();
        }

        @Override
        public void enterSubroutine(@NotNull JetElement subroutine, boolean isFunctionLiteral) {
            Label entryPoint = createUnboundLabel();
            BlockInfo blockInfo = new BlockInfo(subroutine, entryPoint, createUnboundLabel());
            subroutineInfo.push(blockInfo);
            elementToBlockInfo.put(subroutine, blockInfo);
            bindLabel(entryPoint);
        }

        @Override
        public JetElement getCurrentSubroutine() {
            return subroutineInfo.empty() ? null : subroutineInfo.peek().getElement();
        }

        @Override
        public Label getEntryPoint(@NotNull JetElement labelElement) {
            return elementToBlockInfo.get(labelElement).getEntryPoint();
        }

        @Override
        public Label getExitPoint(@NotNull JetElement labelElement) {
            return elementToBlockInfo.get(labelElement).getExitPoint();
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public void exitSubroutine(@NotNull JetElement subroutine, boolean functionLiteral) {
            bindLabel(getExitPoint(subroutine));
            add(new SubroutineExitInstruction(subroutine));
            elementToBlockInfo.remove(subroutine);
        }

        @Override
        public void returnValue(@NotNull JetElement subroutine) {
            add(new ReturnValueInstruction(getExitPoint(subroutine)));
        }

        @Override
        public void returnNoValue(@NotNull JetElement subroutine) {
            add(new ReturnNoValueInstruction(getExitPoint(subroutine)));
        }

        @Override
        public void readNode(@NotNull JetExpression expression) {
            add(new ReadValueInstruction(expression));
        }

        @Override
        public void jump(@NotNull Label label) {
            add(new UnconditionalJumpInstruction(label));
        }

        @Override
        public void jumpOnFalse(@NotNull Label label) {
            add(new ConditionalJumpInstruction(false, label));
        }

        @Override
        public void jumpOnTrue(@NotNull Label label) {
            add(new ConditionalJumpInstruction(true, label));
        }

        @Override
        public void bindLabel(@NotNull Label label) {
            pseudocode.addLabel(label);
        }

        @Override
        public void nondeterministicJump(Label label) {
            add(new NondeterministicJumpInstruction(label));
        }

        @Override
        public void enterTryFinally(@NotNull JetBlockExpression expression) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public void exitTryFinally() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public void unsupported(JetElement element) {
            add(new UnsupportedElementInstruction(element));
        }
    }

}
