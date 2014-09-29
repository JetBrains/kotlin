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

package org.jetbrains.jet.lang.resolve.calls.smartcasts;

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.calls.smartcasts.Nullability.NOT_NULL;

/* package */ class DelegatingDataFlowInfo implements DataFlowInfo {
    private static final ImmutableMap<DataFlowValue,Nullability> EMPTY_NULLABILITY_INFO = ImmutableMap.of();
    private static final SetMultimap<DataFlowValue, JetType> EMPTY_TYPE_INFO = newTypeInfo();

    @Nullable
    private final DataFlowInfo parent;

    @NotNull
    private final ImmutableMap<DataFlowValue, Nullability> nullabilityInfo;

    /** Also immutable */
    @NotNull
    private final SetMultimap<DataFlowValue, JetType> typeInfo;

    /* package */ DelegatingDataFlowInfo(
            @Nullable DataFlowInfo parent,
            @NotNull ImmutableMap<DataFlowValue, Nullability> nullabilityInfo,
            @NotNull SetMultimap<DataFlowValue, JetType> typeInfo
    ) {
        this.parent = parent;
        this.nullabilityInfo = nullabilityInfo;
        this.typeInfo = typeInfo;
    }

    @Override
    @NotNull
    public Map<DataFlowValue, Nullability> getCompleteNullabilityInfo() {
        Map<DataFlowValue, Nullability> result = Maps.newHashMap();
        DelegatingDataFlowInfo info = this;
        while (info != null) {
            for (Map.Entry<DataFlowValue, Nullability> entry : info.nullabilityInfo.entrySet()) {
                DataFlowValue key = entry.getKey();
                Nullability value = entry.getValue();
                if (!result.containsKey(key)) {
                    result.put(key, value);
                }
            }
            info = (DelegatingDataFlowInfo) info.parent;
        }
        return result;
    }

    @Override
    @NotNull
    public SetMultimap<DataFlowValue, JetType> getCompleteTypeInfo() {
        SetMultimap<DataFlowValue, JetType> result = newTypeInfo();
        DelegatingDataFlowInfo info = this;
        while (info != null) {
            for (DataFlowValue key : info.typeInfo.keySet()) {
                result.putAll(key, info.typeInfo.get(key));
            }
            info = (DelegatingDataFlowInfo) info.parent;
        }
        return result;
    }

    @Override
    @NotNull
    public Nullability getNullability(@NotNull DataFlowValue key) {
        if (!key.isStableIdentifier()) return key.getImmanentNullability();
        Nullability nullability = nullabilityInfo.get(key);
        return nullability != null ? nullability :
               parent != null ? parent.getNullability(key) :
               key.getImmanentNullability();
    }

    private boolean putNullability(@NotNull Map<DataFlowValue, Nullability> map, @NotNull DataFlowValue value, @NotNull Nullability nullability) {
        if (!value.isStableIdentifier()) return false;
        map.put(value, nullability);
        return nullability != getNullability(value);
    }

    @Override
    @NotNull
    public Set<JetType> getPossibleTypes(@NotNull DataFlowValue key) {
        Set<JetType> theseTypes = typeInfo.get(key);
        Set<JetType> types = parent == null ? theseTypes : Sets.union(theseTypes, parent.getPossibleTypes(key));
        if (getNullability(key).canBeNull()) {
            return types;
        }

        Set<JetType> enrichedTypes = Sets.newHashSetWithExpectedSize(types.size() + 1);
        JetType originalType = key.getType();
        if (originalType.isNullable()) {
            enrichedTypes.add(TypeUtils.makeNotNullable(originalType));
        }
        for (JetType type : types) {
            enrichedTypes.add(TypeUtils.makeNotNullable(type));
        }

        return enrichedTypes;
    }

    @Override
    @NotNull
    public DataFlowInfo equate(@NotNull DataFlowValue a, @NotNull DataFlowValue b) {
        Map<DataFlowValue, Nullability> builder = Maps.newHashMap();
        Nullability nullabilityOfA = getNullability(a);
        Nullability nullabilityOfB = getNullability(b);

        boolean changed = false;
        changed |= putNullability(builder, a, nullabilityOfA.refine(nullabilityOfB));
        changed |= putNullability(builder, b, nullabilityOfB.refine(nullabilityOfA));
        return changed ? new DelegatingDataFlowInfo(this, ImmutableMap.copyOf(builder), EMPTY_TYPE_INFO) : this;
    }

    @Override
    @NotNull
    public DataFlowInfo disequate(@NotNull DataFlowValue a, @NotNull DataFlowValue b) {
        Map<DataFlowValue, Nullability> builder = Maps.newHashMap();
        Nullability nullabilityOfA = getNullability(a);
        Nullability nullabilityOfB = getNullability(b);

        boolean changed = false;
        changed |= putNullability(builder, a, nullabilityOfA.refine(nullabilityOfB.invert()));
        changed |= putNullability(builder, b, nullabilityOfB.refine(nullabilityOfA.invert()));
        return changed ? new DelegatingDataFlowInfo(this, ImmutableMap.copyOf(builder), EMPTY_TYPE_INFO) : this;
    }

    @Override
    @NotNull
    public DataFlowInfo establishSubtyping(@NotNull DataFlowValue value, @NotNull JetType type) {
        if (value.getType().equals(type)) return this;
        if (getPossibleTypes(value).contains(type)) return this;
        ImmutableMap<DataFlowValue, Nullability> newNullabilityInfo =
                type.isNullable() ? EMPTY_NULLABILITY_INFO : ImmutableMap.of(value, NOT_NULL);
        SetMultimap<DataFlowValue, JetType> newTypeInfo = ImmutableSetMultimap.of(value, type);
        return new DelegatingDataFlowInfo(this, newNullabilityInfo, newTypeInfo);
    }

    @NotNull
    @Override
    public DataFlowInfo and(@NotNull DataFlowInfo otherInfo) {
        if (otherInfo == EMPTY) return this;
        if (this == EMPTY) return otherInfo;
        if (this == otherInfo) return this;

        assert otherInfo instanceof DelegatingDataFlowInfo : "Unknown DataFlowInfo type: " + otherInfo;
        DelegatingDataFlowInfo other = (DelegatingDataFlowInfo) otherInfo;

        Map<DataFlowValue, Nullability> nullabilityMapBuilder = Maps.newHashMap();
        for (Map.Entry<DataFlowValue, Nullability> entry : other.getCompleteNullabilityInfo().entrySet()) {
            DataFlowValue key = entry.getKey();
            Nullability otherFlags = entry.getValue();
            Nullability thisFlags = getNullability(key);
            Nullability flags = thisFlags.and(otherFlags);
            if (flags != thisFlags) {
                nullabilityMapBuilder.put(key, flags);
            }
        }

        SetMultimap<DataFlowValue, JetType> myTypeInfo = getCompleteTypeInfo();
        SetMultimap<DataFlowValue, JetType> otherTypeInfo = other.getCompleteTypeInfo();
        if (nullabilityMapBuilder.isEmpty() && containsAll(myTypeInfo, otherTypeInfo)) {
            return this;
        }

        return new DelegatingDataFlowInfo(this, ImmutableMap.copyOf(nullabilityMapBuilder), otherTypeInfo);
    }

    private static boolean containsAll(SetMultimap<DataFlowValue, JetType> first, SetMultimap<DataFlowValue, JetType> second) {
        return first.entries().containsAll(second.entries());
    }

    @NotNull
    @Override
    public DataFlowInfo or(@NotNull DataFlowInfo otherInfo) {
        if (otherInfo == EMPTY) return EMPTY;
        if (this == EMPTY) return EMPTY;
        if (this == otherInfo) return this;

        assert otherInfo instanceof DelegatingDataFlowInfo : "Unknown DataFlowInfo type: " + otherInfo;
        DelegatingDataFlowInfo other = (DelegatingDataFlowInfo) otherInfo;

        Map<DataFlowValue, Nullability> nullabilityMapBuilder = Maps.newHashMap();
        for (Map.Entry<DataFlowValue, Nullability> entry : other.getCompleteNullabilityInfo().entrySet()) {
            DataFlowValue key = entry.getKey();
            Nullability otherFlags = entry.getValue();
            Nullability thisFlags = getNullability(key);
            nullabilityMapBuilder.put(key, thisFlags.or(otherFlags));
        }

        SetMultimap<DataFlowValue, JetType> myTypeInfo = getCompleteTypeInfo();
        SetMultimap<DataFlowValue, JetType> otherTypeInfo = other.getCompleteTypeInfo();
        SetMultimap<DataFlowValue, JetType> newTypeInfo = newTypeInfo();

        for (DataFlowValue key : Sets.intersection(myTypeInfo.keySet(), otherTypeInfo.keySet())) {
            Set<JetType> thisTypes = myTypeInfo.get(key);
            Set<JetType> otherTypes = otherTypeInfo.get(key);
            newTypeInfo.putAll(key, Sets.intersection(thisTypes, otherTypes));
        }

        if (nullabilityMapBuilder.isEmpty() && newTypeInfo.isEmpty()) {
            return EMPTY;
        }

        return new DelegatingDataFlowInfo(null, ImmutableMap.copyOf(nullabilityMapBuilder), newTypeInfo);
    }

    @NotNull
    /* package */ static SetMultimap<DataFlowValue, JetType> newTypeInfo() {
        return LinkedHashMultimap.create();
    }

    @Override
    public String toString() {
        if (typeInfo.isEmpty() && nullabilityInfo.isEmpty()) {
            return "EMPTY";
        }
        return "Non-trivial DataFlowInfo";
    }
}
