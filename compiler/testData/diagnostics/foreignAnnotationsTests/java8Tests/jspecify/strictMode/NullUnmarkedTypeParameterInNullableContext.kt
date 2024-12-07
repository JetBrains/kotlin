// JSPECIFY_STATE: strict
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    @NullUnmarked
    public static class TargetType<T extends Object> {

        @NullMarked
        public @Nullable T produce() { return null; }

        @NullMarked
        public static TargetType<@Nullable String> TYPE_ARGUMENT() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType<?> UNBOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType<? extends @Nullable String> UPPER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType<? super @Nullable String> LOWER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullMarked
        public static TargetType RAW() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun <T> accept(arg: T) {}

fun test() {
    // jspecify_nullness_mismatch
    accept<String>(<!TYPE_MISMATCH!>NullMarkedType.TargetType.TYPE_ARGUMENT().produce()<!>)
    // jspecify_nullness_mismatch
    accept<Any>(<!TYPE_MISMATCH!>NullMarkedType.TargetType.UNBOUNDED_WILDCARD().produce()<!>)
    // jspecify_nullness_mismatch
    accept<String>(<!TYPE_MISMATCH!>NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().produce()<!>)
    // jspecify_nullness_mismatch
    accept<Any>(<!TYPE_MISMATCH!>NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().produce()<!>)
    // jspecify_nullness_mismatch
    accept<Any>(<!TYPE_MISMATCH!>NullMarkedType.TargetType.RAW().produce()<!>)
}
