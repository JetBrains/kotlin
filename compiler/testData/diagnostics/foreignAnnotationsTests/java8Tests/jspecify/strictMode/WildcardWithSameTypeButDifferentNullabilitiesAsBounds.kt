// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-66947

// FILE: FromJava.java

import org.jspecify.annotations.*;

@NullMarked
public class FromJava {

    public static class NullableTypeParameterUpperBound<T extends @Nullable String> {
        public T produce() { return null; }
        public static NullableTypeParameterUpperBound<? super @Nullable String> NULLABLE_TYPE_ARGUMENT_LOWER_BOUND = new NullableTypeParameterUpperBound<@Nullable String>();
        public static NullableTypeParameterUpperBound<? super @NonNull String> NON_NULL_TYPE_ARGUMENT_LOWER_BOUND = new NullableTypeParameterUpperBound<@Nullable String>();
    }

    public static class NonNullTypeParameterUpperBound<T extends @NonNull String> {
        public T produce() { return null; }
        public static NonNullTypeParameterUpperBound<? super @NonNull String> NON_NULL_TYPE_ARGUMENT_LOWER_BOUND = new NonNullTypeParameterUpperBound<@NonNull String>();
    }

}

// FILE: kotlin.kt

fun test() {
    FromJava.NullableTypeParameterUpperBound.NULLABLE_TYPE_ARGUMENT_LOWER_BOUND.produce()
    FromJava.NullableTypeParameterUpperBound.NON_NULL_TYPE_ARGUMENT_LOWER_BOUND.produce()
    FromJava.NonNullTypeParameterUpperBound.NON_NULL_TYPE_ARGUMENT_LOWER_BOUND.produce()
}
