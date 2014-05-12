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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.*;

public class NondeterministicJumpInstruction extends JetElementInstructionImpl implements JumpInstruction {
    private Instruction next;
    private final List<Label> targetLabels;
    private final Map<Label, Instruction> resolvedTargets;
    private final PseudoValue inputValue;

    public NondeterministicJumpInstruction(
            @NotNull JetElement element,
            List<Label> targetLabels,
            LexicalScope lexicalScope,
            @Nullable PseudoValue inputValue
    ) {
        super(element, lexicalScope);
        this.targetLabels = Lists.newArrayList(targetLabels);
        resolvedTargets = Maps.newLinkedHashMap();
        this.inputValue = inputValue;
    }

    public NondeterministicJumpInstruction(
            @NotNull JetElement element,
            Label targetLabel,
            LexicalScope lexicalScope,
            @Nullable PseudoValue inputValue
    ) {
        this(element, Lists.newArrayList(targetLabel), lexicalScope, inputValue);
    }

    @NotNull
    @Override
    public List<PseudoValue> getInputValues() {
        return Collections.singletonList(inputValue);
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
    public void accept(@NotNull InstructionVisitor visitor) {
        visitor.visitNondeterministicJump(this);
    }

    @Override
    public <R> R accept(@NotNull InstructionVisitorWithResult<R> visitor) {
        return visitor.visitNondeterministicJump(this);
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
        if (inputValue != null) {
            sb.append("|").append(inputValue);
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
        return new NondeterministicJumpInstruction(getElement(), newTargetLabels, lexicalScope, inputValue);
    }
}
