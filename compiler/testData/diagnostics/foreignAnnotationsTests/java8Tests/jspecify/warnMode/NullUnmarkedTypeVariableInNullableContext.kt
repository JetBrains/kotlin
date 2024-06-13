// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER
// MUTE_FOR_PSI_CLASS_FILES_READING
// ^ KT-68389

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    public static class TargetType<T extends @Nullable Object> {

        @NullUnmarked
        public T produce() { return null; }

        public static TargetType<@Nullable String> TYPE_ARGUMENT() { return new TargetType<String>(); }

        public static TargetType<?> UNBOUNDED_WILDCARD() { return new TargetType<String>(); }

        public static TargetType<? extends @Nullable String> UPPER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        public static TargetType<? super @Nullable String> LOWER_BOUNDED_WILDCARD() { return new TargetType<String>(); }

        public static TargetType RAW() { return new TargetType<String>(); }

    }

}

// FILE: kotlin.kt

fun <T> accept(arg: T) {}

fun test() {
    accept<String>(NullMarkedType.TargetType.TYPE_ARGUMENT().produce())
    accept<Any>(NullMarkedType.TargetType.UNBOUNDED_WILDCARD().produce())
    accept<String>(NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().produce())
    accept<Any>(<!TYPE_MISMATCH!>NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().produce()<!>)
    accept<Any>(NullMarkedType.TargetType.RAW().produce())
}
