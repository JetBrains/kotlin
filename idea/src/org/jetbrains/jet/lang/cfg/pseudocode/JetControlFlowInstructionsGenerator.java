package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetControlFlowBuilder;
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
public class JetControlFlowInstructionsGenerator implements JetControlFlowBuilder {

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

    private final Stack<BlockInfo> loopInfo = new Stack<BlockInfo>();
    private final Stack<BlockInfo> subroutineInfo = new Stack<BlockInfo>();
    private final Map<JetElement, BlockInfo> elementToBlockInfo = new HashMap<JetElement, BlockInfo>();

    private int labelCount = 0;

    private final Pseudocode pseudocode = new Pseudocode();

    public Pseudocode getPseudocode() {
        return pseudocode;
    }

    private void add(Instruction instruction) {
        pseudocode.getInstructions().add(instruction);
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
    public void enterSubroutine(@NotNull JetElement subroutine) {
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
    public void exitSubroutine(@NotNull JetElement subroutine) {
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
        add(new ValueInstruction(expression));
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
        pseudocode.getLabels().put(label, pseudocode.getInstructions().size());
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
        throw new IllegalStateException("Unsupported element: " + element.getText() + " " + element);
    }


}
