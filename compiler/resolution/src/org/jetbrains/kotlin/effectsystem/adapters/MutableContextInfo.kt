/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.effectsystem.adapters

import org.jetbrains.kotlin.effectsystem.structure.ESEffect
import org.jetbrains.kotlin.effectsystem.structure.ESValue
import org.jetbrains.kotlin.types.KotlinType

class MutableContextInfo private constructor(
        val firedEffects: MutableList<ESEffect>,
        val deniedEffects: MutableList<ESEffect>,
        val subtypes: MutableMap<ESValue, MutableSet<KotlinType>>,
        val notSubtypes: MutableMap<ESValue, MutableSet<KotlinType>>,
        val equalValues: MutableMap<ESValue, MutableSet<ESValue>>,
        val notEqualValues: MutableMap<ESValue, MutableSet<ESValue>>
) {
    companion object {
        val EMPTY: MutableContextInfo get() = MutableContextInfo(
                firedEffects = mutableListOf(),
                deniedEffects = mutableListOf(),
                subtypes = mutableMapOf(),
                notSubtypes = mutableMapOf(),
                equalValues = mutableMapOf(),
                notEqualValues = mutableMapOf()
        )
    }

    fun subtype(value: ESValue, type: KotlinType) = apply { subtypes.initAndAdd(value, type) }

    fun notSubtype(value: ESValue, type: KotlinType) = apply { notSubtypes.initAndAdd(value, type) }

    fun equal(left: ESValue, right: ESValue) = apply {
        equalValues.initAndAdd(left, right)
        equalValues.initAndAdd(right, left)
    }

    fun notEqual(left: ESValue, right: ESValue) = apply {
        notEqualValues.initAndAdd(left, right)
        notEqualValues.initAndAdd(right, left)
    }

    fun fire(effect: ESEffect) = apply { firedEffects += effect }

    fun deny(effect: ESEffect): MutableContextInfo {
        deniedEffects += effect
        return this
    }

    fun or(other: MutableContextInfo): MutableContextInfo = MutableContextInfo(
        firedEffects = firedEffects.intersect(other.firedEffects).toMutableList(),
        deniedEffects = deniedEffects.intersect(other.deniedEffects).toMutableList(),
        subtypes = subtypes.intersect(other.subtypes),
        notSubtypes = notSubtypes.intersect(other.notSubtypes),
        equalValues = equalValues.intersect(other.equalValues),
        notEqualValues = notEqualValues.intersect(other.notEqualValues)
    )

    fun and(other: MutableContextInfo): MutableContextInfo = MutableContextInfo(
        firedEffects = firedEffects.union(other.firedEffects).toMutableList(),
        deniedEffects = deniedEffects.union(other.deniedEffects).toMutableList(),
        subtypes = subtypes.union(other.subtypes),
        notSubtypes = notSubtypes.union(other.notSubtypes),
        equalValues = equalValues.union(other.equalValues),
        notEqualValues = notEqualValues.union(other.notEqualValues)
    )

    private fun <D> MutableMap<ESValue, MutableSet<D>>.intersect(that: MutableMap<ESValue, MutableSet<D>>): MutableMap<ESValue, MutableSet<D>> {
        val result = mutableMapOf<ESValue, MutableSet<D>>()

        val allKeys = this.keys.intersect(that.keys)
        allKeys.forEach {
            val newValues = this[it]!!.intersect(that[it]!!)
            if (newValues.isNotEmpty()) result[it] = newValues.toMutableSet()
        }
        return result
    }

    private fun <D> Map<ESValue, MutableSet<D>>.union(that: Map<ESValue, MutableSet<D>>): MutableMap<ESValue, MutableSet<D>> {
        val result = mutableMapOf<ESValue, MutableSet<D>>()
        result.putAll(this)
        that.entries.forEach { (thatKey, thatValue) ->
            val oldValue = result[thatKey] ?: mutableSetOf()
            oldValue.addAll(thatValue)
            result[thatKey] = oldValue
        }
        return result
    }

    private fun <D> MutableMap<ESValue, MutableSet<D>>.initAndAdd(key: ESValue, value: D) {
        this.compute(key) { _, maybeValues ->
            val setOfValues = maybeValues ?: mutableSetOf()
            setOfValues.add(value)
            setOfValues
        }
    }

    fun print(): String = buildString {
        val info = this@MutableContextInfo

        fun <D> Map<ESValue, Set<D>>.printMapEntriesWithSeparator(separator: String) {
            this.entries.filter { it.value.isNotEmpty() }.forEach { (key, value) ->
                append(key.toString())
                append(" $separator ")
                appendln(value.toString())
            }
        }

        append("Fired effects: ")
        append(info.firedEffects.joinToString(separator = ", " ))
        appendln("")

        append("Denied effects: ")
        append(info.deniedEffects.joinToString(separator = ", " ))
        appendln()

        subtypes.printMapEntriesWithSeparator("is")

        notSubtypes.printMapEntriesWithSeparator("!is")

        equalValues.printMapEntriesWithSeparator("==")

        notEqualValues.printMapEntriesWithSeparator("!=")

        this.toString()
    }

}