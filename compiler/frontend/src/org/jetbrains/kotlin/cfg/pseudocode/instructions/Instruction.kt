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

package org.jetbrains.kotlin.cfg.pseudocode.instructions

import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue

public trait Instruction {
    public var owner: Pseudocode

    public val previousInstructions: Collection<Instruction>
    public val nextInstructions: Collection<Instruction>

    public val dead: Boolean

    public val lexicalScope: LexicalScope

    public val inputValues: List<PseudoValue>

    public val copies: Collection<Instruction>

    public fun accept(visitor: InstructionVisitor)
    public fun <R> accept(visitor: InstructionVisitorWithResult<R>): R
}
