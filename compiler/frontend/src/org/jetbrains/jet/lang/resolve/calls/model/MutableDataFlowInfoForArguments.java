package org.jetbrains.jet.lang.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;

public interface MutableDataFlowInfoForArguments extends DataFlowInfoForArguments {

    void setInitialDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo);

    void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo);

    MutableDataFlowInfoForArguments WITHOUT_ARGUMENTS_CHECK = new MutableDataFlowInfoForArguments() {
        private DataFlowInfo dataFlowInfo;

        @Override
        public void setInitialDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo) {
            this.dataFlowInfo = dataFlowInfo;
        }

        @Override
        public void updateInfo(
                @NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo
        ) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public DataFlowInfo getResultInfo() {
            return dataFlowInfo;
        }
    };
}
