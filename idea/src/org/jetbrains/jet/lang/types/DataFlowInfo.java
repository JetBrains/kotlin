package org.jetbrains.jet.lang.types;

import com.google.common.base.Supplier;
import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;

import java.util.*;

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

    public static final Supplier<List<JetType>> ARRAY_LIST_SUPPLIER = new Supplier<List<JetType>>() {
        @Override
        public List<JetType> get() {
            return Lists.newArrayList();
        }
    };
    private static DataFlowInfo EMPTY = new DataFlowInfo(ImmutableMap.<VariableDescriptor, NullabilityFlags>of(), Multimaps.newListMultimap(Collections.<VariableDescriptor, Collection<JetType>>emptyMap(), ARRAY_LIST_SUPPLIER));

    public static DataFlowInfo getEmpty() {
        return EMPTY;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ImmutableMap<VariableDescriptor, NullabilityFlags> nullabilityInfo;
    private final ListMultimap<VariableDescriptor, JetType> typeInfo;

    private DataFlowInfo(ImmutableMap<VariableDescriptor, NullabilityFlags> nullabilityInfo, ListMultimap<VariableDescriptor, JetType> typeInfo) {
        this.nullabilityInfo = nullabilityInfo;
        this.typeInfo = typeInfo;
    }

    @Nullable
    public JetType getOutType(@NotNull VariableDescriptor variableDescriptor) {
        JetType outType = variableDescriptor.getOutType();
        if (outType == null) return null;
        if (!outType.isNullable()) return outType;
        NullabilityFlags nullabilityFlags = nullabilityInfo.get(variableDescriptor);
        if (nullabilityFlags != null && !nullabilityFlags.canBeNull()) {
            return TypeUtils.makeNotNullable(outType);
        }
        return outType;
    }

    @NotNull
    public List<JetType> getPossibleTypes(VariableDescriptor variableDescriptor) {
        return typeInfo.get(variableDescriptor);
    }

    public DataFlowInfo equalsToNull(@NotNull VariableDescriptor variableDescriptor, boolean notNull) {
        return new DataFlowInfo(getEqualsToNullMap(variableDescriptor, notNull), typeInfo);
    }

    private ImmutableMap<VariableDescriptor, NullabilityFlags> getEqualsToNullMap(VariableDescriptor variableDescriptor, boolean notNull) {
        Map<VariableDescriptor, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
        builder.put(variableDescriptor, new NullabilityFlags(!notNull, notNull));
        return ImmutableMap.copyOf(builder);
    }

    public DataFlowInfo isInstanceOf(@NotNull VariableDescriptor variableDescriptor, @NotNull JetType type) {
        ListMultimap<VariableDescriptor, JetType> newTypeInfo = copyTypeInfo();
        newTypeInfo.put(variableDescriptor, type);
        return new DataFlowInfo(getEqualsToNullMap(variableDescriptor, !type.isNullable()), newTypeInfo);
    }

    public DataFlowInfo and(DataFlowInfo other) {
        Map<VariableDescriptor, NullabilityFlags> nullabilityMapBuilder = Maps.newHashMap();
        nullabilityMapBuilder.putAll(nullabilityInfo);
        for (Map.Entry<VariableDescriptor, NullabilityFlags> entry : other.nullabilityInfo.entrySet()) {
            VariableDescriptor variableDescriptor = entry.getKey();
            NullabilityFlags otherFlags = entry.getValue();
            NullabilityFlags thisFlags = nullabilityInfo.get(variableDescriptor);
            if (thisFlags != null) {
                nullabilityMapBuilder.put(variableDescriptor, thisFlags.and(otherFlags));
            }
            else {
                nullabilityMapBuilder.put(variableDescriptor, otherFlags);
            }
        }

        ListMultimap<VariableDescriptor, JetType> newTypeInfo = copyTypeInfo();
        newTypeInfo.putAll(other.typeInfo);
        return new DataFlowInfo(ImmutableMap.copyOf(nullabilityMapBuilder), newTypeInfo);
    }

    private ListMultimap<VariableDescriptor, JetType> copyTypeInfo() {
        ListMultimap<VariableDescriptor, JetType> newTypeInfo = Multimaps.newListMultimap(Maps.<VariableDescriptor, Collection<JetType>>newHashMap(), ARRAY_LIST_SUPPLIER);
        newTypeInfo.putAll(typeInfo);
        return newTypeInfo;
    }

    public DataFlowInfo or(DataFlowInfo other) {
        Map<VariableDescriptor, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
        builder.keySet().retainAll(other.nullabilityInfo.keySet());
        for (Map.Entry<VariableDescriptor, NullabilityFlags> entry : builder.entrySet()) {
            VariableDescriptor variableDescriptor = entry.getKey();
            NullabilityFlags thisFlags = entry.getValue();
            NullabilityFlags otherFlags = other.nullabilityInfo.get(variableDescriptor);
            assert (otherFlags != null);
            builder.put(variableDescriptor, thisFlags.or(otherFlags));
        }

        ListMultimap<VariableDescriptor, JetType> newTypeInfo = copyTypeInfo();

        Set<VariableDescriptor> keys = newTypeInfo.keySet();
        keys.retainAll(other.typeInfo.keySet());

        for (VariableDescriptor variableDescriptor : keys) {
            Collection<JetType> thisTypes = typeInfo.get(variableDescriptor);
            Collection<JetType> otherTypes = other.typeInfo.get(variableDescriptor);

            Collection<JetType> newTypes = Sets.newHashSet(thisTypes);
            newTypes.retainAll(otherTypes);

            newTypeInfo.putAll(variableDescriptor, newTypes);
        }

        return new DataFlowInfo(ImmutableMap.copyOf(builder), newTypeInfo);
    }

    private static class NullabilityFlags {
        private final boolean canBeNull;
        private final boolean canBeNonNull;

        private NullabilityFlags(boolean canBeNull, boolean canBeNonNull) {
            this.canBeNull = canBeNull;
            this.canBeNonNull = canBeNonNull;
        }

        public boolean canBeNull() {
            return canBeNull;
        }

        public boolean canBeNonNull() {
            return canBeNonNull;
        }

        public NullabilityFlags and(NullabilityFlags other) {
            return new NullabilityFlags(this.canBeNull && other.canBeNull, this.canBeNonNull && other.canBeNonNull);
        }

        public NullabilityFlags or(NullabilityFlags other) {
            return new NullabilityFlags(this.canBeNull || other.canBeNull, this.canBeNonNull || other.canBeNonNull);
        }

    }
}
