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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import javaslang.Tuple2
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability.NOT_NULL
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.util.javaslang.*
import org.jetbrains.kotlin.utils.newLinkedHashSetWithExpectedSize
import java.util.*

private typealias ImmutableMultimap<K, V> = ImmutableMap<K, ImmutableSet<V>>

private fun <K, V> ImmutableMultimap<K, V>.put(key: K, value: V): ImmutableMultimap<K, V> {
    val oldSet = this[key].getOrElse(ImmutableLinkedHashSet.empty<V>())
    if (oldSet.contains(value)) return this

    return put(key, oldSet.add(value))
}

internal class DataFlowInfoImpl private constructor(
    override val completeNullabilityInfo: ImmutableMap<DataFlowValue, Nullability>,
    override val completeTypeInfo: ImmutableMultimap<DataFlowValue, KotlinType>
) : DataFlowInfo {

    constructor() : this(EMPTY_NULLABILITY_INFO, EMPTY_TYPE_INFO)

    override fun getCollectedNullability(key: DataFlowValue) = getNullability(key, false)

    override fun getStableNullability(key: DataFlowValue) = getNullability(key, true)

    private fun getNullability(key: DataFlowValue, stableOnly: Boolean): Nullability =
        if (stableOnly && !key.isStable) {
            key.immanentNullability
        } else {
            completeNullabilityInfo[key].getOrElse(key.immanentNullability)
        }

    private fun putNullabilityAndTypeInfo(
        map: MutableMap<DataFlowValue, Nullability>,
        value: DataFlowValue,
        nullability: Nullability,
        languageVersionSettings: LanguageVersionSettings,
        newTypeInfoBuilder: SetMultimap<DataFlowValue, KotlinType>? = null,
        // XXX: set to false only as a workaround for OI, see KT-26357 for details (in NI everything works automagically)
        recordUnstable: Boolean = true
    ) {
        if (value.isStable || recordUnstable) {
            map[value] = nullability
        }

        val identifierInfo = value.identifierInfo
        if (!nullability.canBeNull() && languageVersionSettings.supportsFeature(LanguageFeature.SafeCallBoundSmartCasts)) {
            when (identifierInfo) {
                is IdentifierInfo.Qualified -> {
                    val receiverType = identifierInfo.receiverType
                    if (identifierInfo.safe && receiverType != null) {
                        val receiverValue = DataFlowValue(identifierInfo.receiverInfo, receiverType)
                        putNullabilityAndTypeInfo(
                            map, receiverValue, nullability,
                            languageVersionSettings, newTypeInfoBuilder, recordUnstable = recordUnstable
                        )
                    }
                }
                is IdentifierInfo.SafeCast -> {
                    val targetType = identifierInfo.targetType
                    val subjectType = identifierInfo.subjectType
                    if (targetType != null && subjectType != null &&
                        languageVersionSettings.supportsFeature(LanguageFeature.SafeCastCheckBoundSmartCasts)) {

                        val subjectValue = DataFlowValue(identifierInfo.subjectInfo, subjectType)
                        putNullabilityAndTypeInfo(
                            map, subjectValue, nullability,
                            languageVersionSettings, newTypeInfoBuilder, recordUnstable = false
                        )
                        if (subjectValue.isStable) {
                            newTypeInfoBuilder?.put(subjectValue, targetType)
                        }
                    }
                }
                is IdentifierInfo.Variable -> identifierInfo.bound?.let {
                    putNullabilityAndTypeInfo(
                        map, it, nullability,
                        languageVersionSettings, newTypeInfoBuilder, recordUnstable = recordUnstable
                    )
                }
            }
        }
    }


    override fun getCollectedTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings) =
        getCollectedTypes(key, true, languageVersionSettings)

    private fun getCollectedTypes(
        key: DataFlowValue,
        enrichWithNotNull: Boolean,
        languageVersionSettings: LanguageVersionSettings
    ): Set<KotlinType> {
        val types = completeTypeInfo[key].getOrElse(ImmutableLinkedHashSet.empty())
        if (!enrichWithNotNull || getCollectedNullability(key).canBeNull()) {
            return types.toJavaSet()
        }

        val enrichedTypes = newLinkedHashSetWithExpectedSize<KotlinType>(types.size() + 1)
        val originalType = key.type
        types.mapTo(enrichedTypes) { type -> type.makeReallyNotNullIfNeeded(languageVersionSettings) }
        if (originalType.canBeDefinitelyNotNullOrNotNull(languageVersionSettings)) {
            enrichedTypes.add(originalType.makeReallyNotNullIfNeeded(languageVersionSettings))
        }

        return enrichedTypes
    }

    override fun getStableTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings) =
        getStableTypes(key, true, languageVersionSettings)

    private fun getStableTypes(key: DataFlowValue, enrichWithNotNull: Boolean, languageVersionSettings: LanguageVersionSettings) =
        if (!key.isStable) LinkedHashSet() else getCollectedTypes(key, enrichWithNotNull, languageVersionSettings)

    private fun KotlinType.canBeDefinitelyNotNullOrNotNull(settings: LanguageVersionSettings): Boolean {
        return if (settings.supportsFeature(LanguageFeature.NewInference))
            this.isMarkedNullable || DefinitelyNotNullType.makesSenseToBeDefinitelyNotNull(this.unwrap())
        else
            this.isMarkedNullable
    }

    private fun KotlinType.makeReallyNotNullIfNeeded(settings: LanguageVersionSettings): KotlinType {
        return if (settings.supportsFeature(LanguageFeature.NewInference))
            this.unwrap().makeDefinitelyNotNullOrNotNull()
        else
            TypeUtils.makeNotNullable(this)
    }

    /**
     * Call this function to clear all data flow information about
     * the given data flow value.

     * @param value
     */
    override fun clearValueInfo(value: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo {
        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        putNullabilityAndTypeInfo(resultNullabilityInfo, value, value.immanentNullability, languageVersionSettings)
        return create(this, resultNullabilityInfo, EMPTY_TYPE_INFO, value)
    }

    override fun assign(a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo {
        val nullabilityOfB = getStableNullability(b)
        val nullabilityUpdate = mapOf(a to nullabilityOfB)

        var typesForB = getStableTypes(b, languageVersionSettings)
        // Own type of B must be recorded separately, e.g. for a constant
        // But if its type is the same as A, there is no reason to do it
        // because own type is not saved in this set
        // Error types are also not saved
        if (!b.type.isError && a.type != b.type) {
            typesForB += b.type
        }

        return create(this, nullabilityUpdate, listOf(Tuple2(a, typesForB)), a)
    }

    override fun equate(
        a: DataFlowValue, b: DataFlowValue, identityEquals: Boolean, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo = equateOrDisequate(a, b, languageVersionSettings, identityEquals, isEquate = true)

    override fun disequate(
        a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo = equateOrDisequate(a, b, languageVersionSettings, identityEquals = false, isEquate = false)

    private fun equateOrDisequate(
        a: DataFlowValue,
        b: DataFlowValue,
        languageVersionSettings: LanguageVersionSettings,
        identityEquals: Boolean,
        isEquate: Boolean
    ): DataFlowInfo {
        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        val newTypeInfoBuilder = newTypeInfoBuilder()

        val nullabilityOfA = getStableNullability(a)
        val nullabilityOfB = getStableNullability(b)
        val newANullability = nullabilityOfA.refine(if (isEquate) nullabilityOfB else nullabilityOfB.invert())
        val newBNullability = nullabilityOfB.refine(if (isEquate) nullabilityOfA else nullabilityOfA.invert())

        putNullabilityAndTypeInfo(
            resultNullabilityInfo,
            a,
            newANullability,
            languageVersionSettings,
            newTypeInfoBuilder
        )

        putNullabilityAndTypeInfo(
            resultNullabilityInfo,
            b,
            newBNullability,
            languageVersionSettings,
            newTypeInfoBuilder
        )

        var changed = getCollectedNullability(a) != newANullability || getCollectedNullability(b) != newBNullability

        // NB: == has no guarantees of type equality, see KT-11280 for the example
        if (isEquate && (identityEquals || !nullabilityOfA.canBeNonNull() || !nullabilityOfB.canBeNonNull())) {
            newTypeInfoBuilder.putAll(a, getStableTypes(b, false, languageVersionSettings))
            newTypeInfoBuilder.putAll(b, getStableTypes(a, false, languageVersionSettings))
            if (a.type != b.type) {
                // To avoid recording base types of own type
                if (!a.type.isSubtypeOf(b.type)) {
                    newTypeInfoBuilder.put(a, b.type)
                }
                if (!b.type.isSubtypeOf(a.type)) {
                    newTypeInfoBuilder.put(b, a.type)
                }
            }
            changed = changed or !newTypeInfoBuilder.isEmpty
        }

        return if (changed) create(this, resultNullabilityInfo, newTypeInfoBuilder) else this
    }

    override fun establishSubtyping(
        value: DataFlowValue, type: KotlinType, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        if (value.type == type) return this
        if (getCollectedTypes(value, languageVersionSettings).contains(type)) return this
        if (!value.type.isFlexible() && value.type.isSubtypeOf(type)) return this

        val nullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        if (!type.isMarkedNullable) {
            putNullabilityAndTypeInfo(nullabilityInfo, value, NOT_NULL, languageVersionSettings)
        }

        return create(
            this,
            nullabilityInfo,
            listOf(Tuple2(value, listOf(type)))
        )
    }

    override fun and(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return this
        if (this === DataFlowInfo.EMPTY) return other
        if (this === other) return this

        assert(other is DataFlowInfoImpl) { "Unknown DataFlowInfo type: " + other }

        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        for ((key, otherFlags) in other.completeNullabilityInfo) {
            val thisFlags = getCollectedNullability(key)
            val flags = thisFlags.and(otherFlags)
            if (flags != thisFlags) {
                resultNullabilityInfo.put(key, flags)
            }
        }

        val otherTypeInfo = other.completeTypeInfo

        return create(this, resultNullabilityInfo, otherTypeInfo)
    }

    private fun ImmutableSet<KotlinType>?.containsNothing() = this?.any { KotlinBuiltIns.isNothing(it) } ?: false

    private fun ImmutableSet<KotlinType>?.intersectConsideringNothing(other: ImmutableSet<KotlinType>?) =
        when {
            other.containsNothing() -> this
            this.containsNothing() -> other
            else -> this.intersect(other)
        }

    private fun ImmutableSet<KotlinType>?.intersect(other: ImmutableSet<KotlinType>?): ImmutableSet<KotlinType> =
        when {
            this == null -> other ?: ImmutableLinkedHashSet.empty()
            other == null -> this
            else -> this.intersect(other)
        }

    override fun or(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === other) return this

        assert(other is DataFlowInfoImpl) { "Unknown DataFlowInfo type: " + other }

        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        for ((key, otherFlags) in other.completeNullabilityInfo) {
            val thisFlags = getCollectedNullability(key)
            resultNullabilityInfo.put(key, thisFlags.or(otherFlags))
        }

        val myTypeInfo = completeTypeInfo
        val otherTypeInfo = other.completeTypeInfo
        val newTypeInfoBuilder = newTypeInfoBuilder()

        for (key in myTypeInfo.keySet()) {
            if (key in otherTypeInfo.keySet()) {
                newTypeInfoBuilder.putAll(
                    key,
                    myTypeInfo[key].getOrNull().intersectConsideringNothing(otherTypeInfo[key].getOrNull())
                        ?: ImmutableLinkedHashSet.empty()
                )
            }
        }
        return create(null, resultNullabilityInfo, newTypeInfoBuilder)
    }

    override fun toString() = if (completeTypeInfo.isEmpty && completeNullabilityInfo.isEmpty()) "EMPTY" else "Non-trivial DataFlowInfo"

    companion object {
        private val EMPTY_NULLABILITY_INFO: ImmutableMap<DataFlowValue, Nullability> =
            ImmutableHashMap.empty()

        private val EMPTY_TYPE_INFO: ImmutableMultimap<DataFlowValue, KotlinType> =
            ImmutableHashMap.empty()

        private fun newTypeInfoBuilder(): SetMultimap<DataFlowValue, KotlinType> =
            LinkedHashMultimap.create()

        private fun create(
            parent: DataFlowInfo?,
            updatedNullabilityInfo: Map<DataFlowValue, Nullability>,
            updatedTypeInfo: SetMultimap<DataFlowValue, KotlinType>
        ): DataFlowInfo =
            create(
                parent,
                updatedNullabilityInfo,
                updatedTypeInfo.asMap().entries.map { Tuple2(it.key, it.value) }
            )

        private fun create(
            parent: DataFlowInfo?,
            updatedNullabilityInfo: Map<DataFlowValue, Nullability>,
            // NB: typeInfo must be mutable here!
            updatedTypeInfo: Iterable<Tuple2<DataFlowValue, out Iterable<KotlinType>>>,
            valueToClearPreviousTypeInfo: DataFlowValue? = null
        ): DataFlowInfo {
            if (updatedNullabilityInfo.isEmpty() && updatedTypeInfo.none() && valueToClearPreviousTypeInfo == null) {
                return parent ?: DataFlowInfo.EMPTY
            }

            val resultingNullabilityInfo =
                updatedNullabilityInfo.entries.fold(
                    parent?.completeNullabilityInfo ?: EMPTY_NULLABILITY_INFO
                ) { result, (dataFlowValue, nullability) ->
                    if (dataFlowValue.immanentNullability != nullability)
                        result.put(dataFlowValue, nullability)
                    else
                        result.remove(dataFlowValue)
                }

            var resultingTypeInfo = parent?.completeTypeInfo ?: EMPTY_TYPE_INFO

            valueToClearPreviousTypeInfo?.let {
                resultingTypeInfo = resultingTypeInfo.remove(it)
            }

            for ((value, types) in updatedTypeInfo) {
                for (type in types) {
                    if (value.type == type || type.contains { it.constructor is NewCapturedTypeConstructor }) continue
                    resultingTypeInfo = resultingTypeInfo.put(value, type)
                }
            }

            if (resultingNullabilityInfo.isEmpty && resultingTypeInfo.isEmpty) return DataFlowInfo.EMPTY
            if (resultingNullabilityInfo === parent?.completeNullabilityInfo && resultingTypeInfo === parent.completeTypeInfo) {
                return parent
            }

            return DataFlowInfoImpl(resultingNullabilityInfo, resultingTypeInfo)
        }
    }
}
