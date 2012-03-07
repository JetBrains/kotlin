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
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.autocasts.Nullability.NOT_NULL;

/**
 * @author abreslav
 */

public class DataFlowInfo {

    public static abstract class CompositionOperator {
        public abstract DataFlowInfo compose(DataFlowInfo a, DataFlowInfo b);
    }

    public static final CompositionOperator AND = new CompositionOperator() {
        @Override
        public DataFlowInfo compose(DataFlowInfo a, DataFlowInfo b) {
            return a.and(b);
        }
    };

    public static final CompositionOperator OR = new CompositionOperator() {
        @Override
        public DataFlowInfo compose(DataFlowInfo a, DataFlowInfo b) {
            return a.or(b);
        }
    };

    public static DataFlowInfo EMPTY = new DataFlowInfo(ImmutableMap.<DataFlowValue, Nullability>of(), Multimaps.newListMultimap(Collections.<DataFlowValue, Collection<JetType>>emptyMap(), CommonSuppliers.<JetType>getArrayListSupplier()));

    private final ImmutableMap<DataFlowValue, Nullability> nullabilityInfo;
    /** Also immutable */
    private final ListMultimap<DataFlowValue, JetType> typeInfo;

    private DataFlowInfo(ImmutableMap<DataFlowValue, Nullability> nullabilityInfo, ListMultimap<DataFlowValue, JetType> typeInfo) {
        this.nullabilityInfo = nullabilityInfo;
        this.typeInfo = typeInfo;
    }

    @NotNull
    private Nullability getNullability(@NotNull DataFlowValue a) {
        if (!a.isStableIdentifier()) return a.getImmanentNullability();
        Nullability nullability = nullabilityInfo.get(a);
        if (nullability == null) {
            nullability = a.getImmanentNullability();
        }
        return nullability;
    }

    private boolean putNullability(@NotNull Map<DataFlowValue, Nullability> map, @NotNull DataFlowValue value, @NotNull Nullability nullability) {
        if (!value.isStableIdentifier()) return false;
        return map.put(value, nullability) != nullability;
    }

    @NotNull
    public List<JetType> getPossibleTypes(DataFlowValue key) {
        JetType originalType = key.getType();
        List<JetType> types = typeInfo.get(key);
        Nullability nullability = getNullability(key);
        if (nullability.canBeNull()) {
            return types;
        }
        List<JetType> enrichedTypes = Lists.newArrayListWithCapacity(types.size());
        if (originalType.isNullable()) {
            enrichedTypes.add(TypeUtils.makeNotNullable(originalType));
        }
        for (JetType type: types) {
            if (type.isNullable()) {
                enrichedTypes.add(TypeUtils.makeNotNullable(type));
            }
            else {
                enrichedTypes.add(type);
            }
        }
        return enrichedTypes;
    }

    @NotNull
    public DataFlowInfo equate(@NotNull DataFlowValue a, @NotNull DataFlowValue b) {
        Map<DataFlowValue, Nullability> builder = Maps.newHashMap(nullabilityInfo);
        Nullability nullabilityOfA = getNullability(a);
        Nullability nullabilityOfB = getNullability(b);

        boolean changed = false;
        changed |= putNullability(builder, a, nullabilityOfA.refine(nullabilityOfB));
        changed |= putNullability(builder, b, nullabilityOfA.refine(nullabilityOfA));
        return changed ? new DataFlowInfo(ImmutableMap.copyOf(builder), typeInfo) : this;
    }

    @NotNull
    public DataFlowInfo disequate(@NotNull DataFlowValue a, @NotNull DataFlowValue b) {
        Map<DataFlowValue, Nullability> builder = Maps.newHashMap(nullabilityInfo);
        Nullability nullabilityOfA = getNullability(a);
        Nullability nullabilityOfB = getNullability(b);

        boolean changed = false;
        changed |= putNullability(builder, a, nullabilityOfA.refine(nullabilityOfB.invert()));
        changed |= putNullability(builder, b, nullabilityOfB.refine(nullabilityOfA.invert()));
        return changed ? new DataFlowInfo(ImmutableMap.copyOf(builder), typeInfo) : this;
    }

    @NotNull
    public DataFlowInfo establishSubtyping(@NotNull DataFlowValue[] values, @NotNull JetType type) {
        ListMultimap<DataFlowValue, JetType> newTypeInfo = copyTypeInfo();
        Map<DataFlowValue, Nullability> newNullabilityInfo = Maps.newHashMap(nullabilityInfo);
        boolean changed = false;
        for (DataFlowValue value : values) {
//            if (!value.isStableIdentifier()) continue;
            changed = true;
            newTypeInfo.put(value, type);
            if (!type.isNullable()) {
                putNullability(newNullabilityInfo, value, NOT_NULL);
            }
        }
        if (!changed) return this;
        return new DataFlowInfo(ImmutableMap.copyOf(newNullabilityInfo), newTypeInfo);
    }

    private ListMultimap<DataFlowValue, JetType> copyTypeInfo() {
        ListMultimap<DataFlowValue, JetType> newTypeInfo = Multimaps.newListMultimap(Maps.<DataFlowValue, Collection<JetType>>newHashMap(), CommonSuppliers.<JetType>getArrayListSupplier());
        newTypeInfo.putAll(typeInfo);
        return newTypeInfo;
    }

    public DataFlowInfo and(DataFlowInfo other) {
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
        return new DataFlowInfo(ImmutableMap.copyOf(nullabilityMapBuilder), newTypeInfo);
    }

    public DataFlowInfo or(DataFlowInfo other) {
        Map<DataFlowValue, Nullability> builder = Maps.newHashMap(nullabilityInfo);
        builder.keySet().retainAll(other.nullabilityInfo.keySet());
        for (Map.Entry<DataFlowValue, Nullability> entry : builder.entrySet()) {
            DataFlowValue key = entry.getKey();
            Nullability thisFlags = entry.getValue();
            Nullability otherFlags = other.nullabilityInfo.get(key);
            assert (otherFlags != null);
            builder.put(key, thisFlags.or(otherFlags));
        }

        ListMultimap<DataFlowValue, JetType> newTypeInfo = Multimaps.newListMultimap(Maps.<DataFlowValue, Collection<JetType>>newHashMap(), CommonSuppliers.<JetType>getArrayListSupplier());

        Set<DataFlowValue> keys = newTypeInfo.keySet();
        keys.retainAll(other.typeInfo.keySet());

        for (DataFlowValue key : keys) {
            Collection<JetType> thisTypes = typeInfo.get(key);
            Collection<JetType> otherTypes = other.typeInfo.get(key);

            Collection<JetType> newTypes = Sets.newHashSet(thisTypes);
            newTypes.retainAll(otherTypes);

            newTypeInfo.putAll(key, newTypes);
        }

        return new DataFlowInfo(ImmutableMap.copyOf(builder), newTypeInfo);
    }

}

//public class DataFlowInfo {
//
//    public static abstract class CompositionOperator {
//
//        public abstract DataFlowInfo compose(DataFlowInfo a, DataFlowInfo b);
//    }
//    public static final CompositionOperator AND = new CompositionOperator() {
//        @Override
//        public DataFlowInfo compose(DataFlowInfo a, DataFlowInfo b) {
//            return a.and(b);
//        }
//    };
//
//    public static final CompositionOperator OR = new CompositionOperator() {
//        @Override
//        public DataFlowInfo compose(DataFlowInfo a, DataFlowInfo b) {
//            return a.or(b);
//        }
//    };
//
//    private static DataFlowInfo EMPTY = new DataFlowInfo(ImmutableMap.<Object, NullabilityFlags>of(), Multimaps.newListMultimap(Collections.<Object, Collection<JetType>>emptyMap(), CommonSuppliers.<JetType>getArrayListSupplier()));
//
//    public static DataFlowInfo getEmpty() {
//        return EMPTY;
//    }
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//    private final ImmutableMap<Object, NullabilityFlags> nullabilityInfo;
//
//    private final ListMultimap<Object, JetType> typeInfo;
//
//    private DataFlowInfo(ImmutableMap<Object, NullabilityFlags> nullabilityInfo, ListMultimap<Object, JetType> typeInfo) {
//        this.nullabilityInfo = nullabilityInfo;
//        this.typeInfo = typeInfo;
//    }
//
//    @Nullable
//    public JetType getType(@NotNull VariableDescriptor variableDescriptor) {
//        JetType outType = variableDescriptor.getType();
//        if (outType == null) return null;
//        if (!outType.isNullable()) return outType;
//        NullabilityFlags nullabilityFlags = nullabilityInfo.get(variableDescriptor);
//        if (nullabilityFlags != null && !nullabilityFlags.canBeNull()) {
//            return TypeUtils.makeNotNullable(outType);
//        }
//        return outType;
//    }
//
//    @NotNull
//    private List<JetType> getPossibleTypes(Object key, @NotNull JetType originalType) {
//        List<JetType> types = typeInfo.get(key);
//        NullabilityFlags nullabilityFlags = nullabilityInfo.get(key);
//        if (nullabilityFlags == null || nullabilityFlags.canBeNull()) {
//            return types;
//        }
//        List<JetType> enrichedTypes = Lists.newArrayListWithCapacity(types.size());
//        if (originalType.isNullable()) {
//            enrichedTypes.add(TypeUtils.makeNotNullable(originalType));
//        }
//        for (JetType type: types) {
//            if (type.isNullable()) {
//                enrichedTypes.add(TypeUtils.makeNotNullable(type));
//            }
//            else {
//                enrichedTypes.add(type);
//            }
//        }
//        return enrichedTypes;
//    }
//
//    @NotNull
//    public List<JetType> getPossibleTypesForVariable(@NotNull VariableDescriptor variableDescriptor) {
//        return getPossibleTypes(variableDescriptor, variableDescriptor.getType());
//    }
//
//    public List<JetType> getPossibleTypesForReceiver(@NotNull ReceiverDescriptor receiver) {
//        return getPossibleTypes(receiver, receiver.getType());
//    }
//
//    public DataFlowInfo establishEqualityToNull(@NotNull VariableDescriptor variableDescriptor, boolean notNull) {
//        return new DataFlowInfo(getEqualsToNullMap(variableDescriptor, notNull), typeInfo);
//    }
//
//    private ImmutableMap<Object, NullabilityFlags> getEqualsToNullMap(VariableDescriptor variableDescriptor, boolean notNull) {
//        Map<Object, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
//        NullabilityFlags nullabilityFlags = nullabilityInfo.get(variableDescriptor);
//        boolean varNotNull = notNull || (nullabilityFlags != null && !nullabilityFlags.canBeNull);
//        builder.put(variableDescriptor, new NullabilityFlags(!varNotNull, varNotNull));
//        return ImmutableMap.copyOf(builder);
//    }
//
//    private ImmutableMap<Object, NullabilityFlags> getEqualsToNullMap(VariableDescriptor[] variableDescriptors, boolean notNull) {
//        if (variableDescriptors.length == 0) return nullabilityInfo;
//        Map<Object, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
//        for (VariableDescriptor variableDescriptor : variableDescriptors) {
//            if (variableDescriptor != null) {
//                NullabilityFlags nullabilityFlags = nullabilityInfo.get(variableDescriptor);
//                boolean varNotNull = notNull || (nullabilityFlags != null && !nullabilityFlags.canBeNull);
//                builder.put(variableDescriptor, new NullabilityFlags(!varNotNull, varNotNull));
//            }
//        }
//        return ImmutableMap.copyOf(builder);
//    }
//
//    public DataFlowInfo isInstanceOf(@NotNull VariableDescriptor variableDescriptor, @NotNull JetType type) {
//        ListMultimap<Object, JetType> newTypeInfo = copyTypeInfo();
//        newTypeInfo.put(variableDescriptor, type);
//        return new DataFlowInfo(getEqualsToNullMap(variableDescriptor, !type.isNullable()), newTypeInfo);
//    }
//
//    public DataFlowInfo isInstanceOf(@NotNull VariableDescriptor[] variableDescriptors, @NotNull JetType type) {
//        if (variableDescriptors.length == 0) return this;
//        ListMultimap<Object, JetType> newTypeInfo = copyTypeInfo();
//        for (VariableDescriptor variableDescriptor : variableDescriptors) {
//            if (variableDescriptor != null) {
//                newTypeInfo.put(variableDescriptor, type);
//            }
//        }
//        return new DataFlowInfo(getEqualsToNullMap(variableDescriptors, !type.isNullable()), newTypeInfo);
//    }
//
//    public DataFlowInfo and(DataFlowInfo other) {
//        Map<Object, NullabilityFlags> nullabilityMapBuilder = Maps.newHashMap();
//        nullabilityMapBuilder.putAll(nullabilityInfo);
//        for (Map.Entry<Object, NullabilityFlags> entry : other.nullabilityInfo.entrySet()) {
//            Object key = entry.getKey();
//            NullabilityFlags otherFlags = entry.getValue();
//            NullabilityFlags thisFlags = nullabilityInfo.get(key);
//            if (thisFlags != null) {
//                nullabilityMapBuilder.put(key, thisFlags.and(otherFlags));
//            }
//            else {
//                nullabilityMapBuilder.put(key, otherFlags);
//            }
//        }
//
//        ListMultimap<Object, JetType> newTypeInfo = copyTypeInfo();
//        newTypeInfo.putAll(other.typeInfo);
//        return new DataFlowInfo(ImmutableMap.copyOf(nullabilityMapBuilder), newTypeInfo);
//    }
//
//    private ListMultimap<Object, JetType> copyTypeInfo() {
//        ListMultimap<Object, JetType> newTypeInfo = Multimaps.newListMultimap(Maps.<Object, Collection<JetType>>newHashMap(), CommonSuppliers.<JetType>getArrayListSupplier());
//        newTypeInfo.putAll(typeInfo);
//        return newTypeInfo;
//    }
//
//    public DataFlowInfo or(DataFlowInfo other) {
//        Map<Object, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
//        builder.keySet().retainAll(other.nullabilityInfo.keySet());
//        for (Map.Entry<Object, NullabilityFlags> entry : builder.entrySet()) {
//            Object key = entry.getKey();
//            NullabilityFlags thisFlags = entry.getValue();
//            NullabilityFlags otherFlags = other.nullabilityInfo.get(key);
//            assert (otherFlags != null);
//            builder.put(key, thisFlags.or(otherFlags));
//        }
//
//        ListMultimap<Object, JetType> newTypeInfo = Multimaps.newListMultimap(Maps.<Object, Collection<JetType>>newHashMap(), CommonSuppliers.<JetType>getArrayListSupplier());
//
//        Set<Object> keys = newTypeInfo.keySet();
//        keys.retainAll(other.typeInfo.keySet());
//
//        for (Object key : keys) {
//            Collection<JetType> thisTypes = typeInfo.get(key);
//            Collection<JetType> otherTypes = other.typeInfo.get(key);
//
//            Collection<JetType> newTypes = Sets.newHashSet(thisTypes);
//            newTypes.retainAll(otherTypes);
//
//            newTypeInfo.putAll(key, newTypes);
//        }
//
//        return new DataFlowInfo(ImmutableMap.copyOf(builder), newTypeInfo);
//    }
//
//    public DataFlowInfo nullabilityOnly() {
//        return new DataFlowInfo(nullabilityInfo, EMPTY.copyTypeInfo());
//    }
//
//    private static class NullabilityFlags {
//        private final boolean canBeNull;
//        private final boolean canBeNonNull;
//
//        private NullabilityFlags(boolean canBeNull, boolean canBeNonNull) {
//            this.canBeNull = canBeNull;
//            this.canBeNonNull = canBeNonNull;
//        }
//
//        public boolean canBeNull() {
//            return canBeNull;
//        }
//
//        public boolean canBeNonNull() {
//            return canBeNonNull;
//        }
//
//        public NullabilityFlags and(NullabilityFlags other) {
//            return new NullabilityFlags(this.canBeNull && other.canBeNull, this.canBeNonNull && other.canBeNonNull);
//        }
//
//        public NullabilityFlags or(NullabilityFlags other) {
//            return new NullabilityFlags(this.canBeNull || other.canBeNull, this.canBeNonNull || other.canBeNonNull);
//        }
//
//    }
//}
