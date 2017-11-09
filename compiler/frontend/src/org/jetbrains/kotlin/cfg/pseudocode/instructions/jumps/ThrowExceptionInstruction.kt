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

package org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps

import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.cfg.Label
import java.util.Collections
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult

class ThrowExceptionInstruction(
        expression: KtThrowExpression,
        blockScope: BlockScope,
        errorLabel: Label,
        private val thrownValue: PseudoValue
) : AbstractJumpInstruction(expression, errorLabel, blockScope) {
    override val inputValues: List<PseudoValue> get() = Collections.singletonList(thrownValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitThrowExceptionInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitThrowExceptionInstruction(this)

    override fun toString(): String = "throw (${element.text}|$thrownValue)"

    override fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction =
            ThrowExceptionInstruction((element as KtThrowExpression), blockScope, newLabel, thrownValue)
}
