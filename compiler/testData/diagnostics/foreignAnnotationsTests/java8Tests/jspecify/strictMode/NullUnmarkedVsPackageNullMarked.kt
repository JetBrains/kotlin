// FIR_IDENTICAL
// JSPECIFY_STATE: strict
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
    // jspecify_nullness_mismatch
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestB: nullmarkedpackage.UnannotatedType {
    // jspecify_nullness_mismatch
    override fun nullUnmarkedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

fun test(
    a: nullmarkedpackage.NullUnmarkedType,
    b: nullmarkedpackage.UnannotatedType
) {
    // jspecify_nullness_mismatch
    a.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    b.nullUnmarkedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    nullmarkedpackage.UnannotatedTypeWithNullUnmarkedConstructor(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
