// JVM_ABI_K1_K2_DIFF: KT-63955

// FILE: nullabilityAnnotationsOnDelegatedMembers.kt
class JImpl(j: J) : J by j

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
