package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetControlFlowBuilder;
import org.jetbrains.jet.lang.cfg.JetControlFlowBuilderAdapter;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;

import java.util.*;

/**
 * @author abreslav
 */
public class JetControlFlowInstructionsGenerator extends JetControlFlowBuilderAdapter {

    private final Stack<BlockInfo> loopInfo = new Stack<BlockInfo>();
    private final Map<JetElement, BlockInfo> elementToBlockInfo = new HashMap<JetElement, BlockInfo>();
    private int labelCount = 0;

    private final Stack<JetControlFlowInstructionsGeneratorWorker> builders = new Stack<JetControlFlowInstructionsGeneratorWorker>();
    private final JetPseudocodeTrace trace;

    public JetControlFlowInstructionsGenerator(JetPseudocodeTrace trace) {
        super(null);
        this.trace = trace;
    }

    private void pushBuilder(JetElement scopingElement, JetElement subroutine) {
        JetControlFlowInstructionsGeneratorWorker worker = new JetControlFlowInstructionsGeneratorWorker(scopingElement, subroutine);
        builders.push(worker);
        builder = worker;
    }

    private JetControlFlowInstructionsGeneratorWorker popBuilder(@NotNull JetElement element) {
        JetControlFlowInstructionsGeneratorWorker worker = builders.pop();
        trace.recordControlFlowData(element, worker.getPseudocode());
        if (!builders.isEmpty()) {
            builder = builders.peek();
        }
        return worker;
    }

    @Override
    public void enterSubroutine(@NotNull JetElement subroutine, boolean isFunctionLiteral) {
        if (isFunctionLiteral) {
            pushBuilder(subroutine, builder.getCurrentSubroutine());
        }
        else {
            pushBuilder(subroutine, subroutine);
        }
        builder.enterSubroutine(subroutine, false);
    }

    @Override
    public void exitSubroutine(@NotNull JetElement subroutine, boolean functionLiteral) {
        super.exitSubroutine(subroutine, functionLiteral);
        JetControlFlowInstructionsGeneratorWorker worker = popBuilder(subroutine);
        if (functionLiteral) {
            JetControlFlowInstructionsGeneratorWorker builder = builders.peek();
            FunctionLiteralValueInstruction instruction = new FunctionLiteralValueInstruction((JetFunctionLiteralExpression) subroutine);
            instruction.setBody(worker.getPseudocode());
            builder.add(instruction);
        }
    }

    private class JetControlFlowInstructionsGeneratorWorker implements JetControlFlowBuilder {

        private final Pseudocode pseudocode;
        private final JetElement currentSubroutine;

        private JetControlFlowInstructionsGeneratorWorker(@NotNull JetElement scopingElement, @NotNull JetElement currentSubroutine) {
            this.pseudocode = new Pseudocode(scopingElement);
            this.currentSubroutine = currentSubroutine;
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
            return pseudocode.createLabel("l" + labelCount++);
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
//            subroutineInfo.push(blockInfo);
            elementToBlockInfo.put(subroutine, blockInfo);
            bindLabel(entryPoint);
            add(new SubroutineEnterInstruction(subroutine));
        }

        @Override
        public JetElement getCurrentSubroutine() {
            return currentSubroutine;// subroutineInfo.empty() ? null : subroutineInfo.peek().getElement();
        }

        @Override
        public Label getEntryPoint(@NotNull JetElement labelElement) {
            return elementToBlockInfo.get(labelElement).getEntryPoint();
        }

        @Override
        public Label getExitPoint(@NotNull JetElement labelElement) {
            BlockInfo blockInfo = elementToBlockInfo.get(labelElement);
            assert blockInfo != null : labelElement.getText();
            return blockInfo.getExitPoint();
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public void exitSubroutine(@NotNull JetElement subroutine, boolean functionLiteral) {
            bindLabel(getExitPoint(subroutine));
            add(new SubroutineExitInstruction(subroutine));
            elementToBlockInfo.remove(subroutine);
//            subroutineInfo.pop();
        }

        @Override
        public void returnValue(@NotNull JetElement subroutine) {
            add(new ReturnValueInstruction(getExitPoint(subroutine)));
        }

        @Override
        public void returnNoValue(@NotNull JetElement expression, @NotNull JetElement subroutine) {
            add(new ReturnNoValueInstruction(expression, getExitPoint(subroutine)));
        }

        @Override
        public void writeNode(@NotNull JetElement assignment, @NotNull JetElement lValue) {
            add(new WriteValueInstruction(assignment, lValue));
        }

        @Override
        public void readNode(@NotNull JetExpression expression) {
            add(new ReadValueInstruction(expression));
        }

        @Override
        public void readUnit(@NotNull JetExpression expression) {
            add(new ReadUnitValueInstruction(expression));
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
            pseudocode.bindLabel(label);
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
}
