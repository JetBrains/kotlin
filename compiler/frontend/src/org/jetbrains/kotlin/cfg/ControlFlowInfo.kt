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

import javaslang.Tuple2
import org.jetbrains.kotlin.descriptors.VariableDescriptor

typealias ImmutableMap<K, V> = javaslang.collection.Map<K, V>
typealias ImmutableHashMap<K, V> = javaslang.collection.HashMap<K, V>

abstract class ControlFlowInfo<S : ControlFlowInfo<S, D>, D>
internal constructor(
        protected val map: ImmutableMap<VariableDescriptor, D> = ImmutableHashMap.empty()
) : ImmutableMap<VariableDescriptor, D> by map {
    abstract protected fun copy(newMap: ImmutableMap<VariableDescriptor, D>): S

    override fun put(key: VariableDescriptor, value: D): S = put(key, value, this[key].getOrElse(null as D?))

    /**
     * This overload exists just for sake of optimizations: in some cases we've just retrieved the old value,
     * so we don't need to scan through the peristent hashmap again
     */
    fun put(key: VariableDescriptor, value: D, oldValue: D?): S {
        @Suppress("UNCHECKED_CAST")
        // Avoid a copy instance creation if new value is the same
        if (value == oldValue) return this as S
        return copy(map.put(key, value))
    }

    fun retainAll(predicate: (VariableDescriptor) -> Boolean): S = copy(map.removeAll(map.keySet().filterNot(predicate)))

    override fun equals(other: Any?) = map == (other as? ControlFlowInfo<*, *>)?.map

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()
}

operator fun <T> Tuple2<T, *>.component1(): T = _1()
operator fun <T> Tuple2<*, T>.component2(): T = _2()

fun <K, V> ImmutableMap<K, V>.getOrNull(k: K): V? = this[k].getOrElse(null as V?)

class InitControlFlowInfo(map: ImmutableMap<VariableDescriptor, VariableControlFlowState> = ImmutableHashMap.empty()) :
        ControlFlowInfo<InitControlFlowInfo, VariableControlFlowState>(map) {
    override fun copy(newMap: ImmutableMap<VariableDescriptor, VariableControlFlowState>) = InitControlFlowInfo(newMap)

    // this = output of EXHAUSTIVE_WHEN_ELSE instruction
    // merge = input of MergeInstruction
    // returns true if definite initialization in when happens here
    fun checkDefiniteInitializationInWhen(merge: InitControlFlowInfo): Boolean {
        for ((key, value) in iterator()) {
            if (value.initState == InitState.INITIALIZED_EXHAUSTIVELY &&
                merge.getOrNull(key)?.initState == InitState.INITIALIZED) {
                return true
            }
        }
        return false
    }
}

class UseControlFlowInfo(map: ImmutableMap<VariableDescriptor, VariableUseState> = ImmutableHashMap.empty()) :
        ControlFlowInfo<UseControlFlowInfo, VariableUseState>(map) {
    override fun copy(newMap: ImmutableMap<VariableDescriptor, VariableUseState>) = UseControlFlowInfo(newMap)
}

enum class InitState(private val s: String) {
    // Definitely initialized
    INITIALIZED("I"),
    // Fake initializer in else branch of "exhaustive when without else", see MagicKind.EXHAUSTIVE_WHEN_ELSE
    INITIALIZED_EXHAUSTIVELY("IE"),
    // Initialized in some branches, not initialized in other branches
    UNKNOWN("I?"),
    // Definitely not initialized
    NOT_INITIALIZED("");

    fun merge(other: InitState): InitState {
        // X merge X = X
        // X merge IE = IE merge X = X
        // else X merge Y = I?
        if (this == other || other == INITIALIZED_EXHAUSTIVELY) return this
        if (this == INITIALIZED_EXHAUSTIVELY) return other
        return UNKNOWN
    }

    override fun toString() = s
}

class VariableControlFlowState private constructor(val initState: InitState, val isDeclared: Boolean) {

    fun definitelyInitialized(): Boolean {
        return initState == InitState.INITIALIZED
    }

    fun mayBeInitialized(): Boolean {
        return initState != InitState.NOT_INITIALIZED
    }

    override fun toString(): String {
        if (initState == InitState.NOT_INITIALIZED && !isDeclared) return "-"
        return "$initState${if (isDeclared) "D" else ""}"
    }

    companion object {

        private val VS_IT = VariableControlFlowState(InitState.INITIALIZED, true)
        private val VS_IF = VariableControlFlowState(InitState.INITIALIZED, false)
        private val VS_ET = VariableControlFlowState(InitState.INITIALIZED_EXHAUSTIVELY, true)
        private val VS_EF = VariableControlFlowState(InitState.INITIALIZED_EXHAUSTIVELY, false)
        private val VS_UT = VariableControlFlowState(InitState.UNKNOWN, true)
        private val VS_UF = VariableControlFlowState(InitState.UNKNOWN, false)
        private val VS_NT = VariableControlFlowState(InitState.NOT_INITIALIZED, true)
        private val VS_NF = VariableControlFlowState(InitState.NOT_INITIALIZED, false)

        fun create(initState: InitState, isDeclared: Boolean): VariableControlFlowState =
            when (initState) {
                InitState.INITIALIZED -> if (isDeclared) VS_IT else VS_IF
                InitState.INITIALIZED_EXHAUSTIVELY -> if (isDeclared) VS_ET else VS_EF
                InitState.UNKNOWN -> if (isDeclared) VS_UT else VS_UF
                InitState.NOT_INITIALIZED -> if (isDeclared) VS_NT else VS_NF
            }

        fun createInitializedExhaustively(isDeclared: Boolean): VariableControlFlowState {
            return create(InitState.INITIALIZED_EXHAUSTIVELY, isDeclared)
        }

        fun create(isInitialized: Boolean, isDeclared: Boolean = false): VariableControlFlowState {
            return create(if (isInitialized) InitState.INITIALIZED else InitState.NOT_INITIALIZED, isDeclared)
        }

        fun create(isDeclaredHere: Boolean, mergedEdgesData: VariableControlFlowState?): VariableControlFlowState {
            return create(true, isDeclaredHere || mergedEdgesData != null && mergedEdgesData.isDeclared)
        }
    }
}

enum class VariableUseState(private val priority: Int) {
    READ(3),
    WRITTEN_AFTER_READ(2),
    ONLY_WRITTEN_NEVER_READ(1),
    UNUSED(0);

    fun merge(variableUseState: VariableUseState?): VariableUseState {
        if (variableUseState == null || priority > variableUseState.priority) return this
        return variableUseState
    }

    companion object {

        @JvmStatic
        fun isUsed(variableUseState: VariableUseState?): Boolean {
            return variableUseState != null && variableUseState != UNUSED
        }
    }
}

