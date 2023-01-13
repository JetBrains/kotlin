// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode

// FILE: Simple.java
import org.jspecify.annotations.*;

@NullMarked
public class Simple {
    @Nullable public Derived field = null;

    @Nullable
    public Derived foo(Derived x, @NullnessUnspecified Base y) {
        return null;
    }

    public Derived bar() {
        return null;
    }
}

// FILE: Base.java
public class Base {}

// FILE: Derived.java
public class Derived extends Base {
    void foo() {}
}

// FILE: main.kt
fun main(a: Simple, x: Derived): Unit {
    // jspecify_nullness_mismatch
    a.foo(x, null)<!UNSAFE_CALL!>.<!>foo()
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, x)<!UNSAFE_CALL!>.<!>foo()

    a.bar().foo()

    // jspecify_nullness_mismatch
    a.field<!UNSAFE_CALL!>.<!>foo()
}