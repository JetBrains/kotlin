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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability.NOT_NULL
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.newLinkedHashSetWithExpectedSize
import java.util.*

internal class DelegatingDataFlowInfo private constructor(
    private val parent: DataFlowInfo?,
    private val nullabilityInfo: Map<DataFlowValue, Nullability>,
    // Also immutable
    private val typeInfo: SetMultimap<DataFlowValue, KotlinType>,
    /**
     * Value for which type info was cleared or reassigned at this point
     * so parent type info should not be in use
     */
    private val valueWithGivenTypeInfo: DataFlowValue?
) : DataFlowInfo {

    constructor() : this(null, emptyMap(), newTypeInfo(), null)

    override val completeNullabilityInfo: Map<DataFlowValue, Nullability>
        get() {
            val result = hashMapOf<DataFlowValue, Nullability>()
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
        } else {
            nullabilityInfo[key] ?: parent?.getCollectedNullability(key) ?: key.immanentNullability
        }

    private fun putNullabilityAndTypeInfo(
        map: MutableMap<DataFlowValue, Nullability>,
        value: DataFlowValue,
        nullability: Nullability,
        languageVersionSettings: LanguageVersionSettings,
        typeInfo: SetMultimap<DataFlowValue, KotlinType>? = null,
        affectReceiver: Boolean = true,
        // TODO: remove me in version 1.3! I'm very dirty hack!
        // In normal circumstances this should be always true
        recordUnstable: Boolean = true
    ): Boolean {
        if (value.isStable || recordUnstable) {
            map.put(value, nullability)
        }

        val identifierInfo = value.identifierInfo
        if (affectReceiver && !nullability.canBeNull() &&
            languageVersionSettings.supportsFeature(LanguageFeature.SafeCallBoundSmartCasts)) {
            when (identifierInfo) {
                is IdentifierInfo.Qualified -> {
                    val receiverType = identifierInfo.receiverType
                    if (identifierInfo.safe && receiverType != null) {
                        val receiverValue = DataFlowValue(identifierInfo.receiverInfo, receiverType)
                        putNullabilityAndTypeInfo(
                            map, receiverValue, nullability,
                            languageVersionSettings, typeInfo, recordUnstable = recordUnstable
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
                            languageVersionSettings, typeInfo, recordUnstable = false
                        )
                        if (subjectValue.isStable) {
                            typeInfo?.put(subjectValue, targetType)
                        }
                    }
                }
                is IdentifierInfo.Variable -> identifierInfo.bound?.let {
                    putNullabilityAndTypeInfo(
                        map, it, nullability,
                        languageVersionSettings, typeInfo, recordUnstable = recordUnstable
                    )
                }
            }
        }

        return nullability != getCollectedNullability(value)
    }

    override fun getCollectedTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings) =
        getCollectedTypes(key, true, languageVersionSettings)

    private fun getCollectedTypes(
        key: DataFlowValue,
        enrichWithNotNull: Boolean,
        languageVersionSettings: LanguageVersionSettings
    ): Set<KotlinType> {
        val types = collectTypesFromMeAndParents(key, languageVersionSettings)
        if (!enrichWithNotNull || getCollectedNullability(key).canBeNull()) {
            return types
        }

        val enrichedTypes = newLinkedHashSetWithExpectedSize<KotlinType>(types.size + 1)
        val originalType = key.type
        for (type in types) {
            enrichedTypes.add(type.makeReallyNotNullIfNeeded(languageVersionSettings))
        }
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
        val nullability = hashMapOf<DataFlowValue, Nullability>()
        val nullabilityOfB = getStableNullability(b)
        putNullabilityAndTypeInfo(nullability, a, nullabilityOfB, languageVersionSettings, affectReceiver = false)

        val newTypeInfo = newTypeInfo()
        var typesForB = getStableTypes(b, languageVersionSettings)
        // Own type of B must be recorded separately, e.g. for a constant
        // But if its type is the same as A, there is no reason to do it
        // because own type is not saved in this set
        // Error types are also not saved
        if (!b.type.isError && a.type != b.type) {
            typesForB += b.type
        }
        newTypeInfo.putAll(a, typesForB)

        return create(this, nullability, if (newTypeInfo.isEmpty) EMPTY_TYPE_INFO else newTypeInfo, a)
    }

    override fun equate(
        a: DataFlowValue, b: DataFlowValue, identityEquals: Boolean, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        val nullabilityOfA = getStableNullability(a)
        val nullabilityOfB = getStableNullability(b)

        val newTypeInfo = newTypeInfo()
        var changed =
            putNullabilityAndTypeInfo(
                resultNullabilityInfo,
                a,
                nullabilityOfA.refine(nullabilityOfB),
                languageVersionSettings,
                newTypeInfo
            ) or
                    putNullabilityAndTypeInfo(
                        resultNullabilityInfo,
                        b,
                        nullabilityOfB.refine(nullabilityOfA),
                        languageVersionSettings,
                        newTypeInfo
                    )

        // NB: == has no guarantees of type equality, see KT-11280 for the example
        if (identityEquals || !nullabilityOfA.canBeNonNull() || !nullabilityOfB.canBeNonNull()) {
            newTypeInfo.putAll(a, getStableTypes(b, false, languageVersionSettings))
            newTypeInfo.putAll(b, getStableTypes(a, false, languageVersionSettings))
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

        return if (changed) create(this, resultNullabilityInfo, if (newTypeInfo.isEmpty) EMPTY_TYPE_INFO else newTypeInfo) else this
    }

    private fun collectTypesFromMeAndParents(value: DataFlowValue, languageVersionSettings: LanguageVersionSettings): Set<KotlinType> {
        val types = LinkedHashSet<KotlinType>()

        var current: DataFlowInfo? = this
        while (current != null) {
            if (current is DelegatingDataFlowInfo) {
                types.addAll(current.typeInfo.get(value))
                current = if (value == current.valueWithGivenTypeInfo) null else current.parent
            } else {
                types.addAll(current.getCollectedTypes(value, languageVersionSettings))
                break
            }
        }

        return types
    }

    override fun disequate(
        a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        val nullabilityOfA = getStableNullability(a)
        val nullabilityOfB = getStableNullability(b)

        val newTypeInfo = newTypeInfo()
        val changed =
            putNullabilityAndTypeInfo(
                resultNullabilityInfo,
                a,
                nullabilityOfA.refine(nullabilityOfB.invert()),
                languageVersionSettings,
                newTypeInfo
            ) or
                    putNullabilityAndTypeInfo(
                        resultNullabilityInfo,
                        b,
                        nullabilityOfB.refine(nullabilityOfA.invert()),
                        languageVersionSettings,
                        newTypeInfo
                    )

        return if (changed) create(this, resultNullabilityInfo, if (newTypeInfo.isEmpty) EMPTY_TYPE_INFO else newTypeInfo) else this

    }

    override fun establishSubtyping(
        value: DataFlowValue, type: KotlinType, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        if (value.type == type) return this
        if (getCollectedTypes(value, languageVersionSettings).contains(type)) return this
        if (!value.type.isFlexible() && value.type.isSubtypeOf(type)) return this
        val newTypeInfo = newTypeInfo()
        newTypeInfo.put(value, type)
        val nullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        if (!type.isMarkedNullable) {
            putNullabilityAndTypeInfo(nullabilityInfo, value, NOT_NULL, languageVersionSettings)
        }
        return create(this, if (type.isMarkedNullable) emptyMap() else nullabilityInfo, newTypeInfo)
    }

    override fun and(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return this
        if (this === DataFlowInfo.EMPTY) return other
        if (this === other) return this

        assert(other is DelegatingDataFlowInfo) { "Unknown DataFlowInfo type: " + other }

        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        for ((key, otherFlags) in other.completeNullabilityInfo) {
            val thisFlags = getCollectedNullability(key)
            val flags = thisFlags.and(otherFlags)
            if (flags != thisFlags) {
                resultNullabilityInfo.put(key, flags)
            }
        }

        val myTypeInfo = completeTypeInfo
        val otherTypeInfo = other.completeTypeInfo
        if (resultNullabilityInfo.isEmpty() && containsAll(myTypeInfo, otherTypeInfo)) {
            return this
        }

        return create(this, resultNullabilityInfo, otherTypeInfo)
    }

    private fun Set<KotlinType>.containsNothing() = any { KotlinBuiltIns.isNothing(it) }

    private fun Set<KotlinType>.intersectConsideringNothing(other: Set<KotlinType>) =
        when {
            other.containsNothing() -> this
            this.containsNothing() -> other
            else -> this.intersect(other)
        }

    override fun or(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === other) return this

        assert(other is DelegatingDataFlowInfo) { "Unknown DataFlowInfo type: " + other }

        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        for ((key, otherFlags) in other.completeNullabilityInfo) {
            val thisFlags = getCollectedNullability(key)
            resultNullabilityInfo.put(key, thisFlags.or(otherFlags))
        }

        val myTypeInfo = completeTypeInfo
        val otherTypeInfo = other.completeTypeInfo
        val newTypeInfo = newTypeInfo()

        for (key in myTypeInfo.keySet()) {
            if (key in otherTypeInfo.keySet()) {
                newTypeInfo.putAll(key, myTypeInfo[key].intersectConsideringNothing(otherTypeInfo[key]))
            }
        }
        return create(null, resultNullabilityInfo, newTypeInfo)
    }

    override fun toString() = if (typeInfo.isEmpty && nullabilityInfo.isEmpty()) "EMPTY" else "Non-trivial DataFlowInfo"

    companion object {
        private val EMPTY_TYPE_INFO = newTypeInfo()

        private fun containsAll(first: SetMultimap<DataFlowValue, KotlinType>, second: SetMultimap<DataFlowValue, KotlinType>) =
            first.entries().containsAll(second.entries())

        fun newTypeInfo(): SetMultimap<DataFlowValue, KotlinType> = LinkedHashMultimap.create<DataFlowValue, KotlinType>()

        private fun create(
            parent: DataFlowInfo?,
            nullabilityInfo: Map<DataFlowValue, Nullability>,
            // NB: typeInfo must be mutable here!
            typeInfo: SetMultimap<DataFlowValue, KotlinType>,
            valueWithGivenTypeInfo: DataFlowValue? = null
        ): DataFlowInfo {
            val toDelete = newTypeInfo()
            for (value in typeInfo.keys()) {
                for (type in typeInfo[value]) {
                    // Remove original type (see also KT-10666)
                    if (value.type == type || type.contains { it.constructor is NewCapturedTypeConstructor }) {
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
