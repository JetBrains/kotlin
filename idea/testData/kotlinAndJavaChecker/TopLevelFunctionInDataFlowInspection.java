package test;

import org.jetbrains.annotations.NotNull;
import test.kotlin.KotlinPackage;

public class TopLevelFunctionInDataFlowInspection {
    void other(@NotNull Object some) {
        Object foo = KotlinPackage.foo(some);
    }
}

