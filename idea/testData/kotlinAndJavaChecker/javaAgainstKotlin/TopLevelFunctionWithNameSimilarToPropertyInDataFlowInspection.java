package test;

import org.jetbrains.annotations.NotNull;
import test.kotlin.*;

public class TopLevelFunctionWithNameSimilarToPropertyInDataFlowInspection {
    void other(@NotNull Object some) {
        Object foo = (new Test()).foo(some);
    }
}
