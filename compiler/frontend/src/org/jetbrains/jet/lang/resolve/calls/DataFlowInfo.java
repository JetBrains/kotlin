package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.util.CommonSuppliers;

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

    private static DataFlowInfo EMPTY = new DataFlowInfo(ImmutableMap.<Object, NullabilityFlags>of(), Multimaps.newListMultimap(Collections.<Object, Collection<JetType>>emptyMap(), CommonSuppliers.<JetType>getArrayListSupplier()));

    public static DataFlowInfo getEmpty() {
        return EMPTY;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ImmutableMap<Object, NullabilityFlags> nullabilityInfo;

    private final ListMultimap<Object, JetType> typeInfo;

    private DataFlowInfo(ImmutableMap<Object, NullabilityFlags> nullabilityInfo, ListMultimap<Object, JetType> typeInfo) {
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
    private List<JetType> getPossibleTypes(Object key, @NotNull JetType originalType) {
        List<JetType> types = typeInfo.get(key);
        NullabilityFlags nullabilityFlags = nullabilityInfo.get(key);
        if (nullabilityFlags == null || nullabilityFlags.canBeNull()) {
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
    public List<JetType> getPossibleTypesForVariable(@NotNull VariableDescriptor variableDescriptor) {
        return getPossibleTypes(variableDescriptor, variableDescriptor.getOutType());
    }

    public List<JetType> getPossibleTypesForReceiver(@NotNull ReceiverDescriptor receiver) {
        return getPossibleTypes(receiver, receiver.getType());
    }

    public DataFlowInfo equalsToNull(@NotNull VariableDescriptor variableDescriptor, boolean notNull) {
        return new DataFlowInfo(getEqualsToNullMap(variableDescriptor, notNull), typeInfo);
    }

    private ImmutableMap<Object, NullabilityFlags> getEqualsToNullMap(VariableDescriptor variableDescriptor, boolean notNull) {
        Map<Object, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
        NullabilityFlags nullabilityFlags = nullabilityInfo.get(variableDescriptor);
        boolean varNotNull = notNull || (nullabilityFlags != null && !nullabilityFlags.canBeNull);
        builder.put(variableDescriptor, new NullabilityFlags(!varNotNull, varNotNull));
        return ImmutableMap.copyOf(builder);
    }

    private ImmutableMap<Object, NullabilityFlags> getEqualsToNullMap(VariableDescriptor[] variableDescriptors, boolean notNull) {
        if (variableDescriptors.length == 0) return nullabilityInfo;
        Map<Object, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
        for (VariableDescriptor variableDescriptor : variableDescriptors) {
            if (variableDescriptor != null) {
                NullabilityFlags nullabilityFlags = nullabilityInfo.get(variableDescriptor);
                boolean varNotNull = notNull || (nullabilityFlags != null && !nullabilityFlags.canBeNull);
                builder.put(variableDescriptor, new NullabilityFlags(!varNotNull, varNotNull));
            }
        }
        return ImmutableMap.copyOf(builder);
    }

    public DataFlowInfo isInstanceOf(@NotNull VariableDescriptor variableDescriptor, @NotNull JetType type) {
        ListMultimap<Object, JetType> newTypeInfo = copyTypeInfo();
        newTypeInfo.put(variableDescriptor, type);
        return new DataFlowInfo(getEqualsToNullMap(variableDescriptor, !type.isNullable()), newTypeInfo);
    }

    public DataFlowInfo isInstanceOf(@NotNull VariableDescriptor[] variableDescriptors, @NotNull JetType type) {
        if (variableDescriptors.length == 0) return this;
        ListMultimap<Object, JetType> newTypeInfo = copyTypeInfo();
        for (VariableDescriptor variableDescriptor : variableDescriptors) {
            if (variableDescriptor != null) {
                newTypeInfo.put(variableDescriptor, type);
            }
        }
        return new DataFlowInfo(getEqualsToNullMap(variableDescriptors, !type.isNullable()), newTypeInfo);
    }

    public DataFlowInfo and(DataFlowInfo other) {
        Map<Object, NullabilityFlags> nullabilityMapBuilder = Maps.newHashMap();
        nullabilityMapBuilder.putAll(nullabilityInfo);
        for (Map.Entry<Object, NullabilityFlags> entry : other.nullabilityInfo.entrySet()) {
            Object key = entry.getKey();
            NullabilityFlags otherFlags = entry.getValue();
            NullabilityFlags thisFlags = nullabilityInfo.get(key);
            if (thisFlags != null) {
                nullabilityMapBuilder.put(key, thisFlags.and(otherFlags));
            }
            else {
                nullabilityMapBuilder.put(key, otherFlags);
            }
        }

        ListMultimap<Object, JetType> newTypeInfo = copyTypeInfo();
        newTypeInfo.putAll(other.typeInfo);
        return new DataFlowInfo(ImmutableMap.copyOf(nullabilityMapBuilder), newTypeInfo);
    }

    private ListMultimap<Object, JetType> copyTypeInfo() {
        ListMultimap<Object, JetType> newTypeInfo = Multimaps.newListMultimap(Maps.<Object, Collection<JetType>>newHashMap(), CommonSuppliers.<JetType>getArrayListSupplier());
        newTypeInfo.putAll(typeInfo);
        return newTypeInfo;
    }

    public DataFlowInfo or(DataFlowInfo other) {
        Map<Object, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
        builder.keySet().retainAll(other.nullabilityInfo.keySet());
        for (Map.Entry<Object, NullabilityFlags> entry : builder.entrySet()) {
            Object key = entry.getKey();
            NullabilityFlags thisFlags = entry.getValue();
            NullabilityFlags otherFlags = other.nullabilityInfo.get(key);
            assert (otherFlags != null);
            builder.put(key, thisFlags.or(otherFlags));
        }

        ListMultimap<Object, JetType> newTypeInfo = Multimaps.newListMultimap(Maps.<Object, Collection<JetType>>newHashMap(), CommonSuppliers.<JetType>getArrayListSupplier());

        Set<Object> keys = newTypeInfo.keySet();
        keys.retainAll(other.typeInfo.keySet());

        for (Object key : keys) {
            Collection<JetType> thisTypes = typeInfo.get(key);
            Collection<JetType> otherTypes = other.typeInfo.get(key);

            Collection<JetType> newTypes = Sets.newHashSet(thisTypes);
            newTypes.retainAll(otherTypes);

            newTypeInfo.putAll(key, newTypes);
        }

        return new DataFlowInfo(ImmutableMap.copyOf(builder), newTypeInfo);
    }
    
    public DataFlowInfo nullabilityOnly() {
        return new DataFlowInfo(nullabilityInfo, EMPTY.copyTypeInfo());
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
