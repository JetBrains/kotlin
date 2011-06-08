package org.jetbrains.jet.lang.types;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;

/**
 * @author abreslav
 */
public class DataFlowInfo {

    private static DataFlowInfo EMPTY = new DataFlowInfo(ImmutableMap.<VariableDescriptor, NullabilityFlags>of());

    public static DataFlowInfo getEmpty() {
        return EMPTY;
    }

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
        ImmutableMap.Builder<VariableDescriptor, NullabilityFlags> builder = ImmutableMap.builder();
        builder.putAll(nullabilityInfo);
        builder.put(variableDescriptor, new NullabilityFlags(!notNull, notNull));
        return new DataFlowInfo(builder.build());
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
    }
}
