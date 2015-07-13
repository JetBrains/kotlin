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

import java.util.HashSet
import java.util.Collections

// Represents possible integer variable values
public class IntegerVariableValues private constructor() {
    // if true - no values assigned to variable (variable is defined but not initialized)
    public var areEmpty: Boolean = true
        private set
    // if true - analysis can't define variable values
    public var cantBeDefined: Boolean = false
        private set
    public val areDefined: Boolean
        get() = !areEmpty && !cantBeDefined

    private val values: MutableSet<Int> = HashSet()
    // this constant is chosen randomly
    private val undefinedThreshold = 20

    public fun setUndefined() {
        areEmpty = false
        cantBeDefined = true
        values.clear()
    }
    public fun add(value: Int) {
        if(cantBeDefined) {
            return
        }
        values.add(value)
        areEmpty = false
        if(values.size().equals(undefinedThreshold)) {
            setUndefined()
        }
    }
    public fun addAll(values: Collection<Int>) {
        for(value in values) {
            add(value)
        }
    }
    public fun addAll(values: IntegerVariableValues) {
        for(value in values.values) {
            add(value)
        }
    }
    public fun getValues(): Set<Int> = Collections.unmodifiableSet(values)

    override fun equals(other: Any?): Boolean {
        if(other !is IntegerVariableValues) {
            return false
        }
        return areEmpty.equals(other.areEmpty)
               && cantBeDefined.equals(other.cantBeDefined)
               && values.equals(other.values)
    }
    override fun hashCode(): Int {
        var code = 7
        code = 31 * code + areEmpty.hashCode()
        code = 31 * code + cantBeDefined.hashCode()
        code = 31 * code + values.hashCode()
        return code
    }
    override fun toString(): String = values.toString()

    companion object {
        public fun empty(): IntegerVariableValues = IntegerVariableValues()
        public fun singleton(value: Int): IntegerVariableValues {
            val values = empty()
            values.add(value)
            return values
        }
        public fun cantBeDefined(): IntegerVariableValues {
            val values = empty()
            values.setUndefined()
            return values
        }
        public fun ofCollection(collection: Collection<Int>): IntegerVariableValues {
            val values = empty()
            values.addAll(collection)
            return values
        }
    }
}
