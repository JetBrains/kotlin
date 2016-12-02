package test;

import org.jetbrains.annotations.NotNull;
import test.kotlin.*;

public class TopLevelFunctionWithNameSimilarToClassInDataFlowInspection {
    void other(@NotNull Object some) {
        Object foo = TopLevelFunctionWithNameSimilarToClassInDataFlowInspectionKt.foo(some);
    }
}
