// JSPECIFY_STATE: warn
// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType {

    public static class TargetType<
            I extends @NonNull Object,
            O extends @Nullable Object
    > {

        @NullUnmarked
        public O produce() { return null; }

        @NullUnmarked
        public void consume(I arg) {}

        @NullUnmarked
        public static TargetType<String, String> TYPE_ARGUMENT() { return new TargetType<String, String>(); }

        @NullUnmarked
        public static TargetType<?, ?> UNBOUNDED_WILDCARD() { return new TargetType<String, String>(); }

        @NullUnmarked
        public static TargetType<? extends String, ? extends String> UPPER_BOUNDED_WILDCARD() { return new TargetType<String, String>(); }

        @NullUnmarked
        public static TargetType<? super String, ? super String> LOWER_BOUNDED_WILDCARD() { return new TargetType<String, String>(); }

        @NullUnmarked
        public static TargetType RAW() { return new TargetType<String, String>(); }

    }

}

// FILE: kotlin.kt

fun <T> accept(arg: T) {}

fun test() {
    accept<String>(NullMarkedType.TargetType.TYPE_ARGUMENT().produce())
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.TYPE_ARGUMENT().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    accept<Any>(NullMarkedType.TargetType.UNBOUNDED_WILDCARD().produce())
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UNBOUNDED_WILDCARD().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    accept<String>(NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().produce())
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.UPPER_BOUNDED_WILDCARD().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    accept<Any>(NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().produce())
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.LOWER_BOUNDED_WILDCARD().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    // jspecify_nullness_mismatch
    accept<Any>(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>NullMarkedType.TargetType.RAW().produce()<!>)
    // jspecify_nullness_mismatch
    NullMarkedType.TargetType.RAW().consume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
