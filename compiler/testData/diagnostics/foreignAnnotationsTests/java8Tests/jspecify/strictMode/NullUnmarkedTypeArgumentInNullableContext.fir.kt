// JSPECIFY_STATE: strict
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    public static class TargetType<T extends @Nullable Object> {

        public @Nullable T produce() { return null; }

        @NullUnmarked
        public static TargetType<String> INSTANCE() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun <T> accept(arg: T) {}

fun test() {
    // jspecify_nullness_mismatch
    accept<String>(<!ARGUMENT_TYPE_MISMATCH!>NullMarkedType.TargetType.INSTANCE().produce()<!>)
}
