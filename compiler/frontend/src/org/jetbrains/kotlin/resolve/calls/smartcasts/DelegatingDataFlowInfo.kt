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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import com.google.common.collect.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability.NOT_NULL
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.newLinkedHashSetWithExpectedSize
import java.util.*

internal class DelegatingDataFlowInfo private constructor(
        private val parent: DataFlowInfo?,
        private val nullabilityInfo: ImmutableMap<DataFlowValue, Nullability>,
        // Also immutable
        private val typeInfo: SetMultimap<DataFlowValue, KotlinType>,
        /**
         * Value for which type info was cleared or reassigned at this point
         * so parent type info should not be in use
         */
        private val valueWithGivenTypeInfo: DataFlowValue?
) : DataFlowInfo {

    constructor(): this(null, ImmutableMap.of(), newTypeInfo(), null)

    override val completeNullabilityInfo: Map<DataFlowValue, Nullability>
        get() {
            val result = Maps.newHashMap<DataFlowValue, Nullability>()
            var info: DelegatingDataFlowInfo? = this
            while (info != null) {
                for ((key, value) in info.nullabilityInfo) {
                    if (!result.containsKey(key)) {
                        result.put(key, value)
                    }
                }
                info = info.parent as DelegatingDataFlowInfo?
            }
            return result
        }

    override val completeTypeInfo: SetMultimap<DataFlowValue, KotlinType>
        get() {
            val result = newTypeInfo()
            val withGivenTypeInfo = HashSet<DataFlowValue>()
            var info: DelegatingDataFlowInfo? = this
            while (info != null) {
                for (key in info.typeInfo.keySet()) {
                    if (!withGivenTypeInfo.contains(key)) {
                        result.putAll(key, info.typeInfo.get(key))
                    }
                }
                info.valueWithGivenTypeInfo?.let { withGivenTypeInfo.add(it) }
                info = info.parent as DelegatingDataFlowInfo?
            }
            return result
        }

    override fun getCollectedNullability(key: DataFlowValue) = getNullability(key, false)

    override fun getStableNullability(key: DataFlowValue) = getNullability(key, true)

    private fun getNullability(key: DataFlowValue, stableOnly: Boolean) =
            if (stableOnly && !key.isStable) {
                key.immanentNullability
            }
            else {
                nullabilityInfo[key] ?: parent?.getCollectedNullability(key) ?: key.immanentNullability
            }

    private fun putNullability(map: MutableMap<DataFlowValue, Nullability>,
                               value: DataFlowValue,
                               nullability: Nullability,
                               languageVersionSettings: LanguageVersionSettings,
                               affectReceiver: Boolean = true): Boolean {
        map.put(value, nullability)

        val identifierInfo = value.identifierInfo
        if (affectReceiver && !nullability.canBeNull() &&
            languageVersionSettings.supportsFeature(LanguageFeature.SafeCallBoundSmartCasts)) {
            when (identifierInfo) {
                is IdentifierInfo.Qualified -> {
                    val receiverType = identifierInfo.receiverType
                    if (identifierInfo.safe && receiverType != null) {
                        putNullability(map, DataFlowValue(identifierInfo.receiverInfo, receiverType), nullability, languageVersionSettings)
                    }
                }
                is IdentifierInfo.Variable -> identifierInfo.bound?.let {
                    putNullability(map, it, nullability, languageVersionSettings)
                }
            }
        }

        return nullability != getCollectedNullability(value)
    }

    override fun getCollectedTypes(key: DataFlowValue) = getCollectedTypes(key, true)

    private fun getCollectedTypes(key: DataFlowValue, enrichWithNotNull: Boolean): Set<KotlinType> {
        val types = collectTypesFromMeAndParents(key)
        if (!enrichWithNotNull || getCollectedNullability(key).canBeNull()) {
            return types
        }

        val enrichedTypes = Sets.newHashSetWithExpectedSize<KotlinType>(types.size + 1)
        val originalType = key.type
        for (type in types) {
            enrichedTypes.add(TypeUtils.makeNotNullable(type))
        }
        if (originalType.isMarkedNullable) {
            enrichedTypes.add(TypeUtils.makeNotNullable(originalType))
        }

        return enrichedTypes
    }

    override fun getStableTypes(key: DataFlowValue) = getStableTypes(key, true)

    private fun getStableTypes(key: DataFlowValue, enrichWithNotNull: Boolean) =
            if (!key.isStable) LinkedHashSet() else getCollectedTypes(key, enrichWithNotNull)

    /**
     * Call this function to clear all data flow information about
     * the given data flow value.

     * @param value
     */
    override fun clearValueInfo(value: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo {
        val builder = Maps.newHashMap<DataFlowValue, Nullability>()
        putNullability(builder, value, value.immanentNullability, languageVersionSettings)
        return create(this, ImmutableMap.copyOf(builder), EMPTY_TYPE_INFO, value)
    }

    override fun assign(a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo {
        val nullability = Maps.newHashMap<DataFlowValue, Nullability>()
        val nullabilityOfB = getStableNullability(b)
        putNullability(nullability, a, nullabilityOfB, languageVersionSettings, affectReceiver = false)

        val newTypeInfo = newTypeInfo()
        var typesForB = getStableTypes(b)
        // Own type of B must be recorded separately, e.g. for a constant
        // But if its type is the same as A, there is no reason to do it
        // because own type is not saved in this set
        // Error types are also not saved
        if (!b.type.isError && a.type != b.type) {
            typesForB += b.type
        }
        newTypeInfo.putAll(a, typesForB)

        return create(this, ImmutableMap.copyOf(nullability), if (newTypeInfo.isEmpty) EMPTY_TYPE_INFO else newTypeInfo, a)
    }

    override fun equate(
            a: DataFlowValue, b: DataFlowValue, identityEquals: Boolean, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        val builder = Maps.newHashMap<DataFlowValue, Nullability>()
        val nullabilityOfA = getStableNullability(a)
        val nullabilityOfB = getStableNullability(b)

        var changed = putNullability(builder, a, nullabilityOfA.refine(nullabilityOfB), languageVersionSettings) or
                      putNullability(builder, b, nullabilityOfB.refine(nullabilityOfA), languageVersionSettings)

        // NB: == has no guarantees of type equality, see KT-11280 for the example
        val newTypeInfo = newTypeInfo()
        if (identityEquals || !nullabilityOfA.canBeNonNull() || !nullabilityOfB.canBeNonNull()) {
            newTypeInfo.putAll(a, getStableTypes(b, false))
            newTypeInfo.putAll(b, getStableTypes(a, false))
            if (a.type != b.type) {
                // To avoid recording base types of own type
                if (!a.type.isSubtypeOf(b.type)) {
                    newTypeInfo.put(a, b.type)
                }
                if (!b.type.isSubtypeOf(a.type)) {
                    newTypeInfo.put(b, a.type)
                }
            }
            changed = changed or !newTypeInfo.isEmpty
        }

        return if (!changed) {
            this
        }
        else {
            create(this, ImmutableMap.copyOf(builder), if (newTypeInfo.isEmpty) EMPTY_TYPE_INFO else newTypeInfo)
        }
    }

    private fun collectTypesFromMeAndParents(value: DataFlowValue): Set<KotlinType> {
        val types = LinkedHashSet<KotlinType>()

        var current: DataFlowInfo? = this
        while (current != null) {
            if (current is DelegatingDataFlowInfo) {
                types.addAll(current.typeInfo.get(value))
                if (value == current.valueWithGivenTypeInfo) {
                    current = null
                }
                else {
                    current = current.parent
                }
            }
            else {
                types.addAll(current.getCollectedTypes(value))
                break
            }
        }

        return types
    }

    override fun disequate(
            a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        val builder = Maps.newHashMap<DataFlowValue, Nullability>()
        val nullabilityOfA = getStableNullability(a)
        val nullabilityOfB = getStableNullability(b)

        val changed = putNullability(builder, a, nullabilityOfA.refine(nullabilityOfB.invert()), languageVersionSettings) or
                      putNullability(builder, b, nullabilityOfB.refine(nullabilityOfA.invert()), languageVersionSettings)
        return if (changed) create(this, ImmutableMap.copyOf(builder), EMPTY_TYPE_INFO) else this
    }

    override fun establishSubtyping(
            value: DataFlowValue, type: KotlinType, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        if (value.type == type) return this
        if (getCollectedTypes(value).contains(type)) return this
        if (!value.type.isFlexible() && value.type.isSubtypeOf(type)) return this
        val newTypeInfo = newTypeInfo()
        newTypeInfo.put(value, type)
        val builder = Maps.newHashMap<DataFlowValue, Nullability>()
        if (!type.isMarkedNullable) {
            putNullability(builder, value, NOT_NULL, languageVersionSettings)
        }
        val newNullabilityInfo = if (type.isMarkedNullable) EMPTY_NULLABILITY_INFO else ImmutableMap.copyOf(builder)
        return create(this, newNullabilityInfo, newTypeInfo)
    }

    override fun and(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return this
        if (this === DataFlowInfo.EMPTY) return other
        if (this === other) return this

        assert(other is DelegatingDataFlowInfo) { "Unknown DataFlowInfo type: " + other }

        val nullabilityMapBuilder = Maps.newHashMap<DataFlowValue, Nullability>()
        for ((key, otherFlags) in other.completeNullabilityInfo) {
            val thisFlags = getCollectedNullability(key)
            val flags = thisFlags.and(otherFlags)
            if (flags != thisFlags) {
                nullabilityMapBuilder.put(key, flags)
            }
        }

        val myTypeInfo = completeTypeInfo
        val otherTypeInfo = other.completeTypeInfo
        if (nullabilityMapBuilder.isEmpty() && containsAll(myTypeInfo, otherTypeInfo)) {
            return this
        }

        return create(this, ImmutableMap.copyOf(nullabilityMapBuilder), otherTypeInfo)
    }

    private fun Set<KotlinType>.containsNothing() = any { KotlinBuiltIns.isNothing(it) }

    private fun Set<KotlinType>.intersect(other: Set<KotlinType>) =
            if (other.containsNothing()) this
            else if (this.containsNothing()) other
            else Sets.intersection(this, other)

    override fun or(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === other) return this

        assert(other is DelegatingDataFlowInfo) { "Unknown DataFlowInfo type: " + other }

        val nullabilityMapBuilder = Maps.newHashMap<DataFlowValue, Nullability>()
        for ((key, otherFlags) in other.completeNullabilityInfo) {
            val thisFlags = getCollectedNullability(key)
            nullabilityMapBuilder.put(key, thisFlags.or(otherFlags))
        }

        val myTypeInfo = completeTypeInfo
        val otherTypeInfo = other.completeTypeInfo
        val newTypeInfo = newTypeInfo()

        for (key in Sets.intersection(myTypeInfo.keySet(), otherTypeInfo.keySet())) {
            newTypeInfo.putAll(key, myTypeInfo[key].intersect(otherTypeInfo[key]))
        }

        return create(null, ImmutableMap.copyOf(nullabilityMapBuilder), newTypeInfo)
    }

    override fun toString() = if (typeInfo.isEmpty && nullabilityInfo.isEmpty()) "EMPTY" else "Non-trivial DataFlowInfo"

    companion object {
        private val EMPTY_NULLABILITY_INFO = ImmutableMap.of<DataFlowValue, Nullability>()
        private val EMPTY_TYPE_INFO = newTypeInfo()

        private fun containsAll(first: SetMultimap<DataFlowValue, KotlinType>, second: SetMultimap<DataFlowValue, KotlinType>) =
                first.entries().containsAll(second.entries())

        fun newTypeInfo(): SetMultimap<DataFlowValue, KotlinType> = LinkedHashMultimap.create<DataFlowValue, KotlinType>()

        private fun create(parent: DataFlowInfo?,
                           nullabilityInfo: ImmutableMap<DataFlowValue, Nullability>,
                           // NB: typeInfo must be mutable here!
                           typeInfo: SetMultimap<DataFlowValue, KotlinType>,
                           valueWithGivenTypeInfo: DataFlowValue? = null
        ): DataFlowInfo {
            val toDelete = newTypeInfo()
            for (value in typeInfo.keys()) {
                for (type in typeInfo[value]) {
                    // Remove original type (see also KT-10666)
                    if (value.type == type) {
                        toDelete.put(value, type)
                    }
                }
            }
            for ((value, type) in toDelete.entries()) {
                typeInfo.remove(value, type)
            }
            if (nullabilityInfo.isEmpty() && typeInfo.isEmpty && valueWithGivenTypeInfo == null) {
                return parent ?: DataFlowInfoFactory.EMPTY
            }
            return DelegatingDataFlowInfo(parent, nullabilityInfo, typeInfo, valueWithGivenTypeInfo)
        }
    }
}
