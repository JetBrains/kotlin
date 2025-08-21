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
    override fun unannotatedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

interface TestB: nullunmarkedpackage.UnannotatedType {
    override fun nullMarkedProduce(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!>
}

fun test(
    a: nullunmarkedpackage.NullMarkedType,
    b: nullunmarkedpackage.UnannotatedType
) {
    a.unannotatedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b.nullMarkedConsume(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    nullunmarkedpackage.UnannotatedTypeWithNullMarkedConstructor(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
