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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractJumpInstruction extends InstructionImpl {
    private final Label targetLabel;
    private Instruction resolvedTarget;

    public AbstractJumpInstruction(Label targetLabel) {
        this.targetLabel = targetLabel;
    }

    public Label getTargetLabel() {
        return targetLabel;
    }

    public Instruction getResolvedTarget() {
        return resolvedTarget;
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        return Collections.singleton(getResolvedTarget());
    }

    public void setResolvedTarget(Instruction resolvedTarget) {
        this.resolvedTarget = outgoingEdgeTo(resolvedTarget);
    }

    protected abstract AbstractJumpInstruction createCopy(@NotNull Label newLabel);

    final public Instruction copy(@NotNull Label newLabel) {
        return updateCopyInfo(createCopy(newLabel));
    }

    @NotNull
    @Override
    protected Instruction createCopy() {
        return createCopy(targetLabel);
    }
}
