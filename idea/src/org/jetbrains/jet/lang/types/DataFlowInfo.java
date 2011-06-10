package org.jetbrains.jet.lang.types;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;

import java.util.Map;

/**
 * @author abreslav
 */
public class DataFlowInfo {

    private static DataFlowInfo EMPTY = new DataFlowInfo(ImmutableMap.<VariableDescriptor, NullabilityFlags>of());

    public static DataFlowInfo getEmpty() {
        return EMPTY;
    }

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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ImmutableMap<VariableDescriptor, NullabilityFlags> nullabilityInfo;

    public DataFlowInfo(ImmutableMap<VariableDescriptor, NullabilityFlags> nullabilityInfo) {
        this.nullabilityInfo = nullabilityInfo;
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

    public DataFlowInfo equalsToNull(@NotNull VariableDescriptor variableDescriptor, boolean notNull) {
        Map<VariableDescriptor, NullabilityFlags> builder = Maps.newHashMap(nullabilityInfo);
        builder.put(variableDescriptor, new NullabilityFlags(!notNull, notNull));
        return new DataFlowInfo(ImmutableMap.copyOf(builder));
    }

    public DataFlowInfo and(DataFlowInfo other) {
        Map<VariableDescriptor, NullabilityFlags> builder = Maps.newHashMap();
        builder.putAll(nullabilityInfo);
        for (Map.Entry<VariableDescriptor, NullabilityFlags> entry : other.nullabilityInfo.entrySet()) {
            VariableDescriptor variableDescriptor = entry.getKey();
            NullabilityFlags otherFlags = entry.getValue();
            NullabilityFlags thisFlags = nullabilityInfo.get(variableDescriptor);
            if (thisFlags != null) {
                builder.put(variableDescriptor, thisFlags.and(otherFlags));
            }
            else {
                builder.put(variableDescriptor, otherFlags);
            }
        }
        return new DataFlowInfo(ImmutableMap.copyOf(builder));
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
        return new DataFlowInfo(ImmutableMap.copyOf(builder));
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
