// TARGET_BACKEND: JVM

// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ Fake overrides have divirging @EnhancedNullability in K1 and K2

// FILE: delegatedImplementationOfJavaInterface.kt

class Test(private val j: J) : J by j

// FILE: J.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface J {
    void takeNotNull(@NotNull String x);
    void takeNullable(@Nullable String x);
    void takeFlexible(String x);
    @NotNull String returnNotNull();
    @Nullable String returnNullable();
    String returnsFlexible();
}
