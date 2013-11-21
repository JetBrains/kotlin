/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;
import java.util.Set;

public interface DataFlowInfo {
    DataFlowInfo EMPTY = new DelegatingDataFlowInfo(null, ImmutableMap.<DataFlowValue, Nullability>of(),
                                                    DelegatingDataFlowInfo.newTypeInfo());

    @NotNull
    Map<DataFlowValue, Nullability> getCompleteNullabilityInfo();

    @NotNull
    SetMultimap<DataFlowValue, JetType> getCompleteTypeInfo();

    @NotNull
    Nullability getNullability(@NotNull DataFlowValue key);

    @NotNull
    Set<JetType> getPossibleTypes(@NotNull DataFlowValue key);

    @NotNull
    DataFlowInfo equate(@NotNull DataFlowValue a, @NotNull DataFlowValue b);

    @NotNull
    DataFlowInfo disequate(@NotNull DataFlowValue a, @NotNull DataFlowValue b);

    @NotNull
    DataFlowInfo establishSubtyping(@NotNull DataFlowValue value, @NotNull JetType type);

    @NotNull
    DataFlowInfo and(@NotNull DataFlowInfo other);

    @NotNull
    DataFlowInfo or(@NotNull DataFlowInfo other);

    boolean hasTypeInfoConstraints();
}
