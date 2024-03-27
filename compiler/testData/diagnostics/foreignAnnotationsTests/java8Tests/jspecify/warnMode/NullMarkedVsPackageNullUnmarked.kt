// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: nullunmarkedpackage/package-info.java

@NullUnmarked
package nullunmarkedpackage;

import org.jspecify.annotations.*;

// FILE: nullunmarkedpackage/NullMarkedType.java

package nullunmarkedpackage;

import org.jspecify.annotations.*;

@NullMarked
public interface NullMarkedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: nullunmarkedpackage/UnannotatedType.java

package nullunmarkedpackage;

import org.jspecify.annotations.*;

public interface UnannotatedType {
    @NullMarked
    public String nullMarkedProduce();
    @NullMarked
    public void nullMarkedConsume(String arg);
}

// FILE: nullunmarkedpackage/UnannotatedTypeWithNullMarkedConstructor.java

package nullunmarkedpackage;

import org.jspecify.annotations.*;

public class UnannotatedTypeWithNullMarkedConstructor {
    @NullMarked
    public UnannotatedTypeWithNullMarkedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: nullunmarkedpackage.NullMarkedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun unannotatedProduce(): String?
}

interface TestB: nullunmarkedpackage.UnannotatedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun nullMarkedProduce(): String?
}

fun test(
    a: nullunmarkedpackage.NullMarkedType,
    b: nullunmarkedpackage.UnannotatedType
) {
    // jspecify_nullness_mismatch
    a.unannotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    b.nullMarkedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    nullunmarkedpackage.UnannotatedTypeWithNullMarkedConstructor(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
