// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: NullMarkedType.java

import org.jspecify.annotations.*;

@NullMarked
public interface NullMarkedType {

    @NullUnmarked
    public interface NullUnmarkedType {
        public String unannotatedProduce();
        public void unannotatedConsume(String arg);
    }

    public interface UnannotatedType {
        @NullUnmarked
        public String nullUnmarkedProduce();
        @NullUnmarked
        public void nullUnmarkedConsume(String arg);
    }

}

// FILE: NullMarkedTypeWithNullUnmarkedConstructor.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedTypeWithNullUnmarkedConstructor {
    @NullUnmarked
    public NullMarkedTypeWithNullUnmarkedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: NullMarkedType.NullUnmarkedType {
    // jspecify_nullness_mismatch
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestB: NullMarkedType.UnannotatedType {
    // jspecify_nullness_mismatch
    override fun nullUnmarkedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

fun test(
    a: NullMarkedType.NullUnmarkedType,
    b: NullMarkedType.UnannotatedType
) {
    // jspecify_nullness_mismatch
    a.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    b.nullUnmarkedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedTypeWithNullUnmarkedConstructor(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
