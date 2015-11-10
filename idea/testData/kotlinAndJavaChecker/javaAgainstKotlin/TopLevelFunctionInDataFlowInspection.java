package test;

import org.jetbrains.annotations.NotNull;
import test.kotlin.*;

public class TopLevelFunctionInDataFlowInspection {
    void other(@NotNull Object some) {
        Object foo = TopLevelFunctionInDataFlowInspectionKt.foo(some);
    }
}

