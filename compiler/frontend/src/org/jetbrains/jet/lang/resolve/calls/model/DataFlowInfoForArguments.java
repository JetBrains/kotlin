package org.jetbrains.jet.lang.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;

public interface DataFlowInfoForArguments {
    @NotNull
    DataFlowInfo getInfo(@NotNull ValueArgument valueArgument);

    @NotNull
    DataFlowInfo getResultInfo();
}
