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

import io.vavr.collection.CharSeq
import io.vavr.control.Option
import org.jetbrains.kotlin.util.vavr.ImmutableHashMap
import org.jetbrains.kotlin.util.vavr.ImmutableMap
import java.io.PrintStream
import java.io.PrintWriter

interface ReadOnlyControlFlowInfo<K : Any, D : Any> {
    fun getOrNull(key: K): D?
    // Only used in tests
    fun asMap(): ImmutableMap<K, D>
}

abstract class ControlFlowInfo<S : ControlFlowInfo<S, K, D>, K : Any, D : Any>
internal constructor(
    protected val map: ImmutableMap<K, D> = ImmutableHashMap.empty()
) : ImmutableMap<K, D> by map, ReadOnlyControlFlowInfo<K, D> {
    protected abstract fun copy(newMap: ImmutableMap<K, D>): S

    override fun put(key: K, value: D): S = put(key, value, this[key].getOrElse(null as D?))

    /**
     * This overload exists just for sake of optimizations: in some cases we've just retrieved the old value,
     * so we don't need to scan through the persistent hashmap again
     */
    fun put(key: K, value: D, oldValue: D?): S {
        @Suppress("UNCHECKED_CAST")
        // Avoid a copy instance creation if new value is the same
        if (value == oldValue) return this as S
        return copy(map.put(key, value))
    }

    override fun getOrNull(key: K): D? = this[key].getOrElse(null as D?)
    override fun asMap() = this

    fun retainAll(predicate: (K) -> Boolean): S = copy(map.removeAll(map.keySet().filterNot(predicate)))

    override fun equals(other: Any?) = map == (other as? ControlFlowInfo<*, *, *>)?.map

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()

    override fun hasDefiniteSize(): Boolean = map.hasDefiniteSize()
    override fun isTraversableAgain(): Boolean = map.isTraversableAgain
    override fun length(): Int = map.length()
    override fun isDistinct(): Boolean = map.isDistinct
    override fun average(): Option<Double?>? = map.average()
    override fun isEmpty(): Boolean = map.isEmpty
    override fun isOrdered(): Boolean = map.isOrdered
    override fun isSequential(): Boolean = map.isSequential
    override fun isSingleValued(): Boolean = map.isSingleValued
    override fun mkCharSeq(): CharSeq? = map.mkCharSeq()
    override fun mkCharSeq(delimiter: CharSequence?): CharSeq? = map.mkCharSeq(delimiter)
    override fun mkString(): String? = map.mkString()
    override fun mkString(delimiter: CharSequence?): String? = map.mkString(delimiter)
    override fun nonEmpty(): Boolean = map.nonEmpty()
    override fun product(): Number? = map.product()
    override fun sum(): Number? = map.sum()
    override fun eq(o: Any?): Boolean = map.eq(o)
    override fun toCharSeq(): CharSeq? = map.toCharSeq()
    override fun toJavaArray(): Array<out Any?>? = map.toJavaArray()
    override fun arity(): Int = map.arity()
    override fun isMemoized(): Boolean = map.isMemoized

    override fun mkCharSeq(
        prefix: CharSequence?,
        delimiter: CharSequence?,
        suffix: CharSequence?,
    ): CharSeq? = map.mkCharSeq(prefix, delimiter, suffix)

    override fun mkString(prefix: CharSequence?, delimiter: CharSequence?, suffix: CharSequence?): String? =
        map.mkString(prefix, delimiter, suffix)

    override fun out(out: PrintStream?) {
        map.out(out)
    }

    override fun out(writer: PrintWriter?) {
        map.out(writer)
    }

    override fun stderr() {
        map.stderr()
    }

    override fun stdout() {
        map.stdout()
    }
}
