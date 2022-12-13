// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode

// FILE: Defaults.java
import org.jspecify.annotations.*;

@NullMarked
public class Defaults {
    public Foo defaultField = null;
    @Nullable public Foo field = null;

    public Foo everythingNotNullable(Foo x) { return null; }

    public @Nullable Foo everythingNullable(@Nullable Foo x) { return null; }

    public @NullnessUnspecified Foo everythingUnknown(@NullnessUnspecified Foo x) { return null; }

    public @Nullable Foo mixed(Foo x) { return null; }

    public Foo explicitlyNullnessUnspecified(@NullnessUnspecified Foo x) { return null; }
}

// FILE: Foo.java
public class Foo {
    public Object foo() { return null; }
}

// FILE: main.kt
fun main(a: Defaults, x: Foo): Unit {
    // jspecify_nullness_mismatch
    a.everythingNotNullable(<!NULL_FOR_NONNULL_TYPE!>null<!>).foo()
    a.everythingNotNullable(x).foo()

    // jspecify_nullness_mismatch
    a.everythingNullable(null)<!UNSAFE_CALL!>.<!>foo()

    a.everythingUnknown(null).foo()

    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a.mixed(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!UNSAFE_CALL!>.<!>foo()
    // jspecify_nullness_mismatch
    a.mixed(x)<!UNSAFE_CALL!>.<!>foo()

    a.explicitlyNullnessUnspecified(x).foo()
    a.explicitlyNullnessUnspecified(null).foo()

    a.defaultField.foo()

    // jspecify_nullness_mismatch
    a.field<!UNSAFE_CALL!>.<!>foo()
}