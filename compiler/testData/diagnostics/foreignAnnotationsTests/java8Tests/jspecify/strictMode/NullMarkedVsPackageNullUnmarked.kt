// FIR_IDENTICAL
// JSPECIFY_STATE: strict
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
    // jspecify_nullness_mismatch
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestB: nullunmarkedpackage.UnannotatedType {
    // jspecify_nullness_mismatch
    override fun nullMarkedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

fun test(
    a: nullunmarkedpackage.NullMarkedType,
    b: nullunmarkedpackage.UnannotatedType
) {
    // jspecify_nullness_mismatch
    a.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    b.nullMarkedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    nullunmarkedpackage.UnannotatedTypeWithNullMarkedConstructor(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
