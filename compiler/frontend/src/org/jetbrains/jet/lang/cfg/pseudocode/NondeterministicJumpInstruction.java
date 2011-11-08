package org.jetbrains.jet.lang.cfg.pseudocode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;

import java.util.*;

/**
* @author abreslav
* @author svtk
*/
public class NondeterministicJumpInstruction extends InstructionImpl{
    private Instruction next;
    private final List<Label> targetLabels;
    private final Map<Label, Instruction> resolvedTargets;

    public NondeterministicJumpInstruction(List<Label> targetLabels) {
        this.targetLabels = Lists.newArrayList(targetLabels);
        resolvedTargets = Maps.newLinkedHashMap();
    }

    public NondeterministicJumpInstruction(Label targetLabel) {
        this(Lists.newArrayList(targetLabel));
    }

    public List<Label> getTargetLabels() {
        return targetLabels;
    }

    public Map<Label, Instruction> getResolvedTargets() {
        return resolvedTargets;
    }

    public void setResolvedTarget(Label label, Instruction resolvedTarget) {
        Instruction target = outgoingEdgeTo(resolvedTarget);
        resolvedTargets.put(label, target);
    }

    public Instruction getNext() {
        return next;
    }
    public void setNext(Instruction next) {
        this.next = outgoingEdgeTo(next);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitNondeterministicJump(this);
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        ArrayList<Instruction> targetInstructions = Lists.newArrayList(getResolvedTargets().values());
        targetInstructions.add(getNext());
        return targetInstructions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("jmp?(");
        for (Iterator<Label> iterator = targetLabels.iterator(); iterator.hasNext(); ) {
            Label targetLabel = iterator.next();
            sb.append(targetLabel.getName());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
