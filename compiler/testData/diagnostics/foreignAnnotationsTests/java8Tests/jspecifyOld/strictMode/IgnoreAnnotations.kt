// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode

// FILE: IgnoreAnnotations.java
import org.jspecify.nullness.*;

@NullMarked
public class IgnoreAnnotations {
    @Nullable public Derived field = null;

    @Nullable
    public Derived foo(Derived x, @NullnessUnspecified Base y) {
        return null;
    }

    public Derived everythingNotNullable(Derived x) { return null; }

    public @Nullable Derived everythingNullable(@Nullable Derived x) { return null; }

    public @NullnessUnspecified Derived everythingUnknown(@NullnessUnspecified Derived x) { return null; }
}

// FILE: Base.java
public class Base {
    void foo() {}
}

// FILE: Derived.java
public class Derived extends Base { }

// FILE: main.kt
fun main(a: IgnoreAnnotations, x: Derived): Unit {
    // jspecify_nullness_mismatch
    a.foo(x, null)<!UNSAFE_CALL!>.<!>foo()
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, x)<!UNSAFE_CALL!>.<!>foo()

    // jspecify_nullness_mismatch
    a.field<!UNSAFE_CALL!>.<!>foo()

    // jspecify_nullness_mismatch
    a.everythingNotNullable(<!NULL_FOR_NONNULL_TYPE!>null<!>).foo()
    a.everythingNotNullable(x).foo()

    // jspecify_nullness_mismatch
    a.everythingNullable(null)<!UNSAFE_CALL!>.<!>foo()

    a.everythingUnknown(null).foo()
}