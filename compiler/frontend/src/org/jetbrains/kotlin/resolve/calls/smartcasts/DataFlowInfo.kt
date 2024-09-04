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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.javaslang.ImmutableMap
import org.jetbrains.kotlin.util.javaslang.ImmutableSet

/**
 * This interface is intended to provide and edit information about value nullabilities and possible types.
 * Data flow info is immutable so functions never change it.
 */
interface DataFlowInfo {

    val completeNullabilityInfo: ImmutableMap<DataFlowValue, Nullability>

    val completeTypeInfo: ImmutableMap<DataFlowValue, ImmutableSet<KotlinType>>

    /**
     * Returns collected nullability for the given value, NOT taking its stability into account.
     */
    fun getCollectedNullability(key: DataFlowValue): Nullability

    /**
     * Returns collected nullability for the given value if it's stable.
     * Otherwise basic value nullability is returned
     */
    fun getStableNullability(key: DataFlowValue): Nullability

    /**
     * Returns possible types for the given value, NOT taking its stability into account.
     *
     * IMPORTANT: by default, the original (native) type for this value
     * are NOT included. So it's quite possible to get an empty set here.
     * Also, type order in the result set MAKES SENSE so keep it stable and do not change without reason
     */
    fun getCollectedTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings): Set<KotlinType>

    /**
     * Returns possible types for the given value if it's stable.
     * Otherwise, basic value type is returned.
     *
     * IMPORTANT: by default, the original (native) type for this value
     * are NOT included. So it's quite possible to get an empty set here.
     * Also, type order in the result set MAKES SENSE so keep it stable and do not change without reason
     */
    fun getStableTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings): Set<KotlinType>

    /**
     * Call this function to clear all data flow information about
     * the given data flow value. Useful when we are not sure how this value can be changed, e.g. in a loop.
     */
    fun clearValueInfo(value: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    /**
     * Call this function when b is assigned to a
     */
    fun assign(a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    /**
     * Call this function when it's known than a == b.
     */
    fun equate(a: DataFlowValue, b: DataFlowValue, identityEquals: Boolean, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    /**
     * Call this function when it's known than a != b
     */
    fun disequate(a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    fun establishSubtyping(value: DataFlowValue, type: KotlinType, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    /**
     * Call this function to add data flow information from other to this and return sum as the result
     */
    fun and(other: DataFlowInfo): DataFlowInfo

    /**
     * Call this function to choose data flow information common for this and other and return it as the result
     */
    fun or(other: DataFlowInfo): DataFlowInfo

    companion object {
        val EMPTY = DataFlowInfoFactory.EMPTY
    }
}

object DataFlowInfoFactory {
    @JvmField
    val EMPTY: DataFlowInfo = DataFlowInfoImpl()
}
