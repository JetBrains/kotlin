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

import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import java.util.*

public data class ValuesData(
        val intVarsToValues: MutableMap<VariableDescriptor, IntegerVariableValues> = HashMap(),
        val intFakeVarsToValues: MutableMap<PseudoValue, IntegerVariableValues> = HashMap(),
        val boolVarsToValues: MutableMap<VariableDescriptor, BooleanVariableValue> = HashMap(),
        val boolFakeVarsToValues: MutableMap<PseudoValue, BooleanVariableValue> = HashMap()
) {
    override fun toString(): String {
        val ints = "I${MapUtils.mapToString(intVarsToValues, { it.getName().asString() })}"
        val fakeInts = "FI${MapUtils.mapToString(intFakeVarsToValues, { it.debugName })}"
        val bools = "B${MapUtils.mapToString(boolVarsToValues, { it.getName().asString() })}"
        val fakeBools = "FB${MapUtils.mapToString(boolFakeVarsToValues, { it.debugName })}"
        return "$ints|$fakeInts|$bools|$fakeBools"
    }

    public fun copy(): ValuesData {
        fun copyIntsMap<K>(m: MutableMap<K, IntegerVariableValues>) =
                HashMap(m.map { Pair(it.getKey(), it.getValue().copy()) }.toMap())
        return ValuesData(
                copyIntsMap(intVarsToValues),
                copyIntsMap(intFakeVarsToValues),
                HashMap(boolVarsToValues),
                HashMap(boolFakeVarsToValues)
        )
    }
}