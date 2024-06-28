// JSPECIFY_STATE: strict
// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedType<ParametricNullnessTypeParameter extends @Nullable Object> {

    @NullUnmarked
    public class NullUnmarkedType {
        public ParametricNullnessTypeParameter unannotatedProduce() { return null; }
        public void unannotatedConsume(ParametricNullnessTypeParameter arg) {}
    }

    public class UnannotatedType {
        @NullUnmarked
        public ParametricNullnessTypeParameter nullUnmarkedProduce() { return null; }
        @NullUnmarked
        public void nullUnmarkedConsume(ParametricNullnessTypeParameter arg) {}
    }

    public void test(
        NullUnmarkedType a,
        UnannotatedType b
    ) {}

}

// FILE: kotlin.kt

fun <T> accept(arg: T) {}

class Test<ParametricNullnessTypeParameter>: NullMarkedType<ParametricNullnessTypeParameter>() {

    override fun test(
        a: NullUnmarkedType,
        b: UnannotatedType
    ) {
        // jspecify_nullness_mismatch
        accept<ParametricNullnessTypeParameter & Any>(<!TYPE_MISMATCH!>a.unannotatedProduce()<!>)
        // jspecify_nullness_mismatch
        a.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)

        // jspecify_nullness_mismatch
        accept<ParametricNullnessTypeParameter & Any>(<!TYPE_MISMATCH!>b.nullUnmarkedProduce()<!>)
        // jspecify_nullness_mismatch
        b.nullUnmarkedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    }

}
