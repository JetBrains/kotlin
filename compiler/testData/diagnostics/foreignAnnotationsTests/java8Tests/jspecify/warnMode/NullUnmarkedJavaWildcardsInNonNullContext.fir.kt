// JSPECIFY_STATE: warn

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
    NullMarkedType.TargetType.UNBOUNDED_WILDCARD().consume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().consume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().consume(<!TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
