// FIR_IDENTICAL
// JSPECIFY_STATE: warn

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    public static class TargetType<T extends Object> {

        public void consume(T arg) {}

        @NullUnmarked
        public static TargetType INSTANCE() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun test() {
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.INSTANCE().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
