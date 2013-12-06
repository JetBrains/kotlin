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

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

public abstract class InstructionImpl implements Instruction {
    private Pseudocode owner;
    private final Collection<Instruction> previousInstructions = new LinkedHashSet<Instruction>();
    private final Collection<Instruction> copies = Sets.newHashSet();
    private Instruction original;
    protected boolean isDead = false;

    protected InstructionImpl() {
    }

    @Override
    @NotNull
    public Pseudocode getOwner() {
        return owner;
    }

    @Override
    public void setOwner(@NotNull Pseudocode owner) {
        assert this.owner == null || this.owner == owner;
        this.owner = owner;
    }

    @NotNull
    @Override
    public Collection<Instruction> getPreviousInstructions() {
        return previousInstructions;
    }

    @Nullable
    protected Instruction outgoingEdgeTo(@Nullable Instruction target) {
        if (target != null) {
            target.getPreviousInstructions().add(this);
        }
        return target;
    }

    public void die() {
        isDead = true;
    }
    
    public boolean isDead() {
        return isDead;
    }

    public final Instruction copy() {
        return updateCopyInfo(createCopy());
    }

    @NotNull
    protected abstract Instruction createCopy();

    @NotNull
    @Override
    public Collection<Instruction> getCopies() {
        if (original != null) {
            Collection<Instruction> originalCopies = Sets.newHashSet(original.getCopies());
            originalCopies.remove(this);
            originalCopies.add(original);
            return originalCopies;
        }
        return copies;
    }

    private void addCopy(@NotNull Instruction instruction) {
        copies.add(instruction);
    }

    private void setOriginal(@NotNull Instruction original) {
        assert this.original == null :
                "Instruction can't have two originals: this.original = " + this.original + "; new original = " + original;
        this.original = original;
    }

    protected Instruction updateCopyInfo(@NotNull Instruction instruction) {
        addCopy(instruction);
        ((InstructionImpl)instruction).setOriginal(this);
        return instruction;
    }
}
