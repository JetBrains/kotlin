// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// JSPECIFY_STATE: warn

// FILE: Foo.java
import org.jspecify.annotations.Nullable;

public class Foo {
    public static <T> void gauge(@Nullable T stateObject) {}
}

// FILE: main.kt
fun <T> test(metric: T) {
    if (metric is String) {
        Foo.gauge(metric)
    }
}
