// FIR_IDENTICAL
// JSPECIFY_STATE: warn

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
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(x, null)<!>.foo()
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, x)<!>.foo()

    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.foo()

    // jspecify_nullness_mismatch
    a.everythingNotNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).foo()
    a.everythingNotNullable(x).foo()

    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.everythingNullable(null)<!>.foo()

    a.everythingUnknown(null).foo()
}