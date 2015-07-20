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

package org.jetbrains.kotlin.cfg

import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

public interface BooleanVariableValue {
    public object True : BooleanVariableValue

    public object False : BooleanVariableValue

    public data class Undefined (
            val onTrueRestrictions: Map<VariableDescriptor, IntegerVariableValues>,
            val onFalseRestrictions: Map<VariableDescriptor, IntegerVariableValues>
    ): BooleanVariableValue

    companion object {
        public val undefinedWithNoRestrictions: Undefined = Undefined(mapOf(), mapOf())
        public fun trueOrFalse(value: Boolean): BooleanVariableValue = if(value) True else False
    }
}