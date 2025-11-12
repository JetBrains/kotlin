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
    NullMarkedType.TargetType.TYPE_ARGUMENT().consume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    NullMarkedType.TargetType.UNBOUNDED_WILDCARD().consume(null)
    NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().consume(null)
    NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().consume(null)
    NullMarkedType.TargetType.RAW().consume(null)
}
