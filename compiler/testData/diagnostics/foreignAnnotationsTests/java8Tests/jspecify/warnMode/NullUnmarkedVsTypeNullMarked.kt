// FIR_IDENTICAL
// JSPECIFY_STATE: warn
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
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun unannotatedProduce(): String?
}

interface TestB: NullMarkedType.UnannotatedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun nullUnmarkedProduce(): String?
}

fun test(
    a: NullMarkedType.NullUnmarkedType,
    b: NullMarkedType.UnannotatedType
) {
    // jspecify_nullness_mismatch
    a.unannotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    b.nullUnmarkedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    NullMarkedTypeWithNullUnmarkedConstructor(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
