// FIR_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    @NullUnmarked
    public static class TargetType<T extends Object> {

        @NullMarked
        public void consume(T arg) {}

        @NullMarked
        public static TargetType<String> TYPE_ARGUMENT() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType<?> UNBOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType<? extends String> UPPER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType<? super String> LOWER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType RAW() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun test() {
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.TYPE_ARGUMENT().consume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UNBOUNDED_WILDCARD().consume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().consume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().consume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    NullMarkedType.TargetType.RAW().consume(null)
}
