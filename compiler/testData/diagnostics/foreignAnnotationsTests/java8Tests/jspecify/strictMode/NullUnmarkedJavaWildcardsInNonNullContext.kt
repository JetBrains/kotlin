// FIR_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    public static class TargetType<T extends Object> {

        public void consume(T arg) {}

        @NullUnmarked
        public static TargetType<?> UNBOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullUnmarked
        public static TargetType<? extends String> UPPER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullUnmarked
        public static TargetType<? super String> LOWER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun test() {
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UNBOUNDED_WILDCARD().consume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().consume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().consume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
