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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.SubroutineEnterInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.SubroutineExitInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.SubroutineSinkInstruction;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.List;
import java.util.Set;

public interface Pseudocode {
    @NotNull
    JetElement getCorrespondingElement();

    @Nullable
    Pseudocode getParent();

    @NotNull
    Set<LocalFunctionDeclarationInstruction> getLocalDeclarations();

    @NotNull
    List<Instruction> getInstructions();

    @NotNull
    List<Instruction> getReversedInstructions();

    @NotNull
    List<Instruction> getInstructionsIncludingDeadCode();

    @NotNull
    SubroutineExitInstruction getExitInstruction();

    @NotNull
    SubroutineSinkInstruction getSinkInstruction();

    @NotNull
    SubroutineEnterInstruction getEnterInstruction();

    @Nullable
    PseudoValue getElementValue(@Nullable JetElement element);

    @NotNull
    List<? extends JetElement> getValueElements(@Nullable PseudoValue value);

    @NotNull
    List<? extends Instruction> getUsages(@Nullable PseudoValue value);

    boolean isSideEffectFree(@NotNull Instruction instruction);
}
