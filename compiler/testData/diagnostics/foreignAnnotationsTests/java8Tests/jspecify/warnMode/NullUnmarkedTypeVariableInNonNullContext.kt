// JSPECIFY_STATE: warn

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    public static class TargetType<T extends Object> {

        @NullUnmarked
        public void consume(T arg) {}

        public static TargetType<String> TYPE_ARGUMENT() { return new TargetType<String>(); }

        public static TargetType<?> UNBOUNDED_WILDCARD() { return new TargetType<String>(); }

        public static TargetType<? extends String> UPPER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        public static TargetType<? super String> LOWER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        public static TargetType RAW() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun test() {
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.TYPE_ARGUMENT().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UNBOUNDED_WILDCARD().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().consume(null)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.RAW().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
