/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.autocasts.Nullability.NOT_NULL;

/* package */ class DelegatingDataFlowInfo implements DataFlowInfo {
    @Nullable
    private final DataFlowInfo parent;

    @NotNull
    private final ImmutableMap<DataFlowValue, Nullability> nullabilityInfo;

    /** Also immutable */
    @NotNull
    private final ListMultimap<DataFlowValue, JetType> typeInfo;

    private static final ImmutableMap<DataFlowValue,Nullability> EMPTY_NULLABILITY_INFO =
            ImmutableMap.copyOf(Collections.<DataFlowValue, Nullability>emptyMap());
    private static final ListMultimap<DataFlowValue, JetType> EMPTY_TYPE_INFO = newTypeInfo();

    /* package */ DelegatingDataFlowInfo(
            @Nullable DataFlowInfo parent,
            @NotNull ImmutableMap<DataFlowValue, Nullability> nullabilityInfo,
            @NotNull ListMultimap<DataFlowValue, JetType> typeInfo
    ) {
        this.parent = parent;
        this.nullabilityInfo = nullabilityInfo;
        this.typeInfo = typeInfo;
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
    public List<JetType> getPossibleTypes(@NotNull DataFlowValue key) {
        List<JetType> types = typeInfo.get(key);
        if (getNullability(key).canBeNull()) {
            if (parent != null) {
                types = Lists.newArrayList(types);
                addAllUnique(types, parent.getPossibleTypes(key));
            }
            return types;
        }

        List<JetType> enrichedTypes = Lists.newArrayListWithCapacity(types.size() + 1);
        JetType originalType = key.getType();
        if (originalType.isNullable()) {
            enrichedTypes.add(TypeUtils.makeNotNullable(originalType));
        }
        for (JetType type : types) {
            enrichedTypes.add(TypeUtils.makeNotNullable(type));
        }

        if (parent != null) {
            addAllUnique(enrichedTypes, parent.getPossibleTypes(key));
        }

        return enrichedTypes;
    }

    private static void addAllUnique(@NotNull List<JetType> toList, @NotNull List<JetType> fromList) {
        for (JetType type : fromList) {
            if (!toList.contains(type)) {
                toList.add(type);
            }
        }
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
        ListMultimap<DataFlowValue, JetType> newTypeInfo = ImmutableListMultimap.of(value, type);
        return new DelegatingDataFlowInfo(this, newNullabilityInfo, newTypeInfo);
    }

    @NotNull
    private ListMultimap<DataFlowValue, JetType> copyTypeInfo() {
        ListMultimap<DataFlowValue, JetType> newTypeInfo = newTypeInfo();
        newTypeInfo.putAll(typeInfo);
        return newTypeInfo;
    }

    @NotNull
    @Override
    public DataFlowInfo and(@NotNull DataFlowInfo otherInfo) {
        if (otherInfo == EMPTY) return this;

        assert otherInfo instanceof DelegatingDataFlowInfo : "Unknown DataFlowInfo type: " + otherInfo;
        DelegatingDataFlowInfo other = (DelegatingDataFlowInfo) otherInfo;

        Map<DataFlowValue, Nullability> nullabilityMapBuilder = Maps.newHashMap();
        nullabilityMapBuilder.putAll(nullabilityInfo);
        for (Map.Entry<DataFlowValue, Nullability> entry : other.nullabilityInfo.entrySet()) {
            DataFlowValue key = entry.getKey();
            Nullability otherFlags = entry.getValue();
            Nullability thisFlags = nullabilityInfo.get(key);
            if (thisFlags != null) {
                nullabilityMapBuilder.put(key, thisFlags.and(otherFlags));
            }
            else {
                nullabilityMapBuilder.put(key, otherFlags);
            }
        }

        ListMultimap<DataFlowValue, JetType> newTypeInfo = copyTypeInfo();
        newTypeInfo.putAll(other.typeInfo);
        return new DelegatingDataFlowInfo(null, ImmutableMap.copyOf(nullabilityMapBuilder), newTypeInfo);
    }

    @NotNull
    @Override
    public DataFlowInfo or(@NotNull DataFlowInfo otherInfo) {
        if (otherInfo == EMPTY) return EMPTY;

        assert otherInfo instanceof DelegatingDataFlowInfo : "Unknown DataFlowInfo type: " + otherInfo;
        DelegatingDataFlowInfo other = (DelegatingDataFlowInfo) otherInfo;

        Map<DataFlowValue, Nullability> builder = Maps.newHashMap(nullabilityInfo);
        builder.keySet().retainAll(other.nullabilityInfo.keySet());
        for (Map.Entry<DataFlowValue, Nullability> entry : builder.entrySet()) {
            DataFlowValue key = entry.getKey();
            Nullability thisFlags = entry.getValue();
            Nullability otherFlags = other.nullabilityInfo.get(key);
            assert (otherFlags != null);
            builder.put(key, thisFlags.or(otherFlags));
        }

        ListMultimap<DataFlowValue, JetType> newTypeInfo = newTypeInfo();

        Set<DataFlowValue> keys = Sets.newHashSet(typeInfo.keySet());
        keys.retainAll(other.typeInfo.keySet());

        for (DataFlowValue key : keys) {
            Collection<JetType> thisTypes = typeInfo.get(key);
            Collection<JetType> otherTypes = other.typeInfo.get(key);

            Collection<JetType> newTypes = Sets.newHashSet(thisTypes);
            newTypes.retainAll(otherTypes);

            newTypeInfo.putAll(key, newTypes);
        }

        return new DelegatingDataFlowInfo(null, ImmutableMap.copyOf(builder), newTypeInfo);
    }

    @NotNull
    /* package */ static ListMultimap<DataFlowValue, JetType> newTypeInfo() {
        return Multimaps.newListMultimap(Maps.<DataFlowValue, Collection<JetType>>newHashMap(), CommonSuppliers.<JetType>getArrayListSupplier());
    }

    @Override
    public boolean hasTypeInfoConstraints() {
        return !typeInfo.isEmpty();
    }

    @Override
    public String toString() {
        if (typeInfo.isEmpty() && nullabilityInfo.isEmpty()) {
            return "EMPTY";
        }
        return "Non-trivial DataFlowInfo";
    }
}
