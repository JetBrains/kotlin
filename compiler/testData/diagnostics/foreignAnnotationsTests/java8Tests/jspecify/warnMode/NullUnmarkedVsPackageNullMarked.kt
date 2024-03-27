// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: nullmarkedpackage/package-info.java

@NullMarked
package nullmarkedpackage;

import org.jspecify.annotations.*;

// FILE: nullmarkedpackage/NullUnmarkedType.java

package nullmarkedpackage;

import org.jspecify.annotations.*;

@NullUnmarked
public interface NullUnmarkedType {
    public String unannotatedProduce();
    public void unannotatedConsume(String arg);
}

// FILE: nullmarkedpackage/UnannotatedType.java

package nullmarkedpackage;

import org.jspecify.annotations.*;

public interface UnannotatedType {
    @NullUnmarked
    public String nullUnmarkedProduce();
    @NullUnmarked
    public void nullUnmarkedConsume(String arg);
}

// FILE: nullmarkedpackage/UnannotatedTypeWithNullUnmarkedConstructor.java

package nullmarkedpackage;

import org.jspecify.annotations.*;

public class UnannotatedTypeWithNullUnmarkedConstructor {
    @NullUnmarked
    public UnannotatedTypeWithNullUnmarkedConstructor(String arg) {}
}

// FILE: kotlin.kt

interface TestA: nullmarkedpackage.NullUnmarkedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun unannotatedProduce(): String?
}

interface TestB: nullmarkedpackage.UnannotatedType {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun nullUnmarkedProduce(): String?
}

fun test(
    a: nullmarkedpackage.NullUnmarkedType,
    b: nullmarkedpackage.UnannotatedType
) {
    // jspecify_nullness_mismatch
    a.unannotatedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    b.nullUnmarkedConsume(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    // jspecify_nullness_mismatch
    nullmarkedpackage.UnannotatedTypeWithNullUnmarkedConstructor(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
}
