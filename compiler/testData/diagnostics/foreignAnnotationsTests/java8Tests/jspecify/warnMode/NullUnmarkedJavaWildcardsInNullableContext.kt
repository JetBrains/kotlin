// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    public static class TargetType<T extends @Nullable Object> {

        public @Nullable T produce() { return null; }

        @NullUnmarked
        public static TargetType<?> UNBOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullUnmarked
        public static TargetType<? extends String> UPPER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        @NullUnmarked
        public static TargetType<? super String> LOWER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun <T> accept(arg: T) {}

fun test() {
    accept<Any>(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>NullMarkedType.TargetType.UNBOUNDED_WILDCARD().produce()<!>)
    accept<String>(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().produce()<!>)
    accept<Any>(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().produce()<!>)
}
