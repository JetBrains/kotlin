/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;

import java.util.*;

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
        List<Instruction> targetInstructions = Lists.newArrayList(getResolvedTargets().values());
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

    @NotNull
    @Override
    protected Instruction createCopy() {
        return createCopy(getTargetLabels());
    }

    @NotNull
    public final Instruction copy(@NotNull List<Label> newTargetLabels) {
        return updateCopyInfo(createCopy(newTargetLabels));
    }

    private Instruction createCopy(@NotNull List<Label> newTargetLabels) {
        return new NondeterministicJumpInstruction(newTargetLabels);
    }
}
