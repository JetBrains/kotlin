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

package org.jetbrains.kotlin.cfg.outofbound

import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.cfg.outofbound.IntegerVariableValues
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

public interface BooleanVariableValue {
    public object True : BooleanVariableValue {
        override fun toString(): String = "True"
    }

    public object False : BooleanVariableValue {
        override fun toString(): String = "False"
    }

    public data class Undefined (
            val onTrueRestrictions: Map<VariableDescriptor, Set<Int>>,
            val onFalseRestrictions: Map<VariableDescriptor, Set<Int>>
    ): BooleanVariableValue {
        override fun toString(): String {
            val descriptorToString: (VariableDescriptor) -> String = { it.getName().asString() }
            val ontTrue = MapUtils.mapToString(onTrueRestrictions, descriptorToString, descriptorToString)
            val ontFalse = MapUtils.mapToString(onFalseRestrictions, descriptorToString, descriptorToString)
            return "Undef:[onTrue:$ontTrue, onFalse:$ontFalse]"
        }
    }

    // For now derived classes of BooleanVariableValue are immutable,
    // so copy returns this. In the future, if some class become mutable
    // the implementation of this method may change
    public fun copy(): BooleanVariableValue = this

    companion object {
        public val undefinedWithNoRestrictions: Undefined = Undefined(mapOf(), mapOf())
        public fun create(value: Boolean): BooleanVariableValue = if(value) True else False
    }
}