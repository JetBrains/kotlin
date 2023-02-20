// FIR_IDENTICAL
// FULL_JDK

// FILE: ContainerUtil.java

import org.jetbrains.annotations.NotNull;
import java.util.Set;

public class ContainerUtil {
    public static @NotNull <T> Set<T> set(T @NotNull ... items) {
        return null;
    }
}

// FILE: test.kt

class Some

fun foo(some: Some) {
    // K1: Ok
    // K2 (Unit test): CCE from the compiler while trying to report NAMED_ARGUMENT_NOT_ALLOWED
    // K2 (intellij build) : NAMED_ARGUMENT_NOT_ALLOWED
    ContainerUtil.set(some)
}
