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

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.util.javaslang.ImmutableHashMap
import org.jetbrains.kotlin.util.javaslang.ImmutableMap

interface ReadOnlyControlFlowInfo<D : Any> {
    fun getOrNull(variableDescriptor: VariableDescriptor): D?
    // Only used in tests
    fun asMap(): ImmutableMap<VariableDescriptor, D>
}

abstract class ControlFlowInfo<S : ControlFlowInfo<S, D>, D : Any>
internal constructor(
    protected val map: ImmutableMap<VariableDescriptor, D> = ImmutableHashMap.empty()
) : ImmutableMap<VariableDescriptor, D> by map, ReadOnlyControlFlowInfo<D> {
    protected abstract fun copy(newMap: ImmutableMap<VariableDescriptor, D>): S

    override fun put(key: VariableDescriptor, value: D): S = put(key, value, this[key].getOrElse(null as D?))

    /**
     * This overload exists just for sake of optimizations: in some cases we've just retrieved the old value,
     * so we don't need to scan through the persistent hashmap again
     */
    fun put(key: VariableDescriptor, value: D, oldValue: D?): S {
        @Suppress("UNCHECKED_CAST")
        // Avoid a copy instance creation if new value is the same
        if (value == oldValue) return this as S
        return copy(map.put(key, value))
    }

    override fun getOrNull(variableDescriptor: VariableDescriptor): D? = this[variableDescriptor].getOrElse(null as D?)
    override fun asMap() = this

    fun retainAll(predicate: (VariableDescriptor) -> Boolean): S = copy(map.removeAll(map.keySet().filterNot(predicate)))

    override fun equals(other: Any?) = map == (other as? ControlFlowInfo<*, *>)?.map

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()
}