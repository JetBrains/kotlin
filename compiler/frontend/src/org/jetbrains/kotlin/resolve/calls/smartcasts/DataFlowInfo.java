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

package org.jetbrains.kotlin.resolve.calls.smartcasts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.types.JetType;

import java.util.Map;
import java.util.Set;

/**
 * This interface is intended to provide and edit information about value nullabilities and possible types.
 * Data flow info is immutable so functions never change it.
 */
public interface DataFlowInfo {
    DataFlowInfo EMPTY = new DelegatingDataFlowInfo(null, ImmutableMap.<DataFlowValue, Nullability>of(), DelegatingDataFlowInfo.newTypeInfo());

    @NotNull
    Map<DataFlowValue, Nullability> getCompleteNullabilityInfo();

    @NotNull
    SetMultimap<DataFlowValue, JetType> getCompleteTypeInfo();

    @NotNull
    Nullability getNullability(@NotNull DataFlowValue key);

    /**
     * Gets a set of all possible types for this value, INCLUDING the original (native) type
     */
    @NotNull
    Set<JetType> getPossibleTypes(@NotNull DataFlowValue key, boolean withOriginalType);

    /**
     * IMPORTANT: by default, the original (native) type for this value
     * are NOT included. So it's quite possible to get an empty set here.
     * If you need also the original type, use getPossibleTypes(key, true)
     */
    @NotNull
    Set<JetType> getPossibleTypes(@NotNull DataFlowValue key);

    /**
     * Call this function to clear all data flow information about
     * the given data flow value. Useful when we are not sure how this value can be changed, e.g. in a loop.
     */
    @NotNull
    DataFlowInfo clearValueInfo(@NotNull DataFlowValue value);

    /**
     * Call this function when b is assigned to a
     */
    @NotNull
    DataFlowInfo assign(@NotNull DataFlowValue a, @NotNull DataFlowValue b);

    /**
     * Call this function when it's known than a == b
     */
    @NotNull
    DataFlowInfo equate(@NotNull DataFlowValue a, @NotNull DataFlowValue b);

    /**
     * Call this function when it's known than a != b
     */
    @NotNull
    DataFlowInfo disequate(@NotNull DataFlowValue a, @NotNull DataFlowValue b);

    @NotNull
    DataFlowInfo establishSubtyping(@NotNull DataFlowValue value, @NotNull JetType type);

    /**
     * Call this function to add data flow information from other to this and return sum as the result
     */
    @NotNull
    DataFlowInfo and(@NotNull DataFlowInfo other);

    /**
     * Call this function to choose data flow information common for this and other and return it as the result
     */
    @NotNull
    DataFlowInfo or(@NotNull DataFlowInfo other);
}
