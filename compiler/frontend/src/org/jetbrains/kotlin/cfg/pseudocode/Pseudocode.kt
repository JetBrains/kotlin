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

package org.jetbrains.kotlin.cfg.pseudocode

import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineEnterInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineExitInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction
import org.jetbrains.kotlin.psi.KtElement

interface Pseudocode {
    val correspondingElement: KtElement

    val parent: Pseudocode?

    val localDeclarations: Set<LocalFunctionDeclarationInstruction>

    val instructions: List<Instruction>

    val reversedInstructions: List<Instruction>

    val instructionsIncludingDeadCode: List<Instruction>

    val exitInstruction: SubroutineExitInstruction

    val errorInstruction: SubroutineExitInstruction

    val sinkInstruction: SubroutineSinkInstruction

    val enterInstruction: SubroutineEnterInstruction

    fun getElementValue(element: KtElement?): PseudoValue?

    fun getValueElements(value: PseudoValue?): List<KtElement>

    fun getUsages(value: PseudoValue?): List<Instruction>

    fun isSideEffectFree(instruction: Instruction): Boolean

    fun copy(): Pseudocode

    fun instructionForElement(element: KtElement): KtElementInstruction?
}
