// FIR_IDENTICAL
// JSPECIFY_STATE: warn

// FILE: Defaults.java
import org.jspecify.nullness.*;

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
    a.everythingNotNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).foo()
    a.everythingNotNullable(x).foo()

    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.everythingNullable(null)<!>.foo()

    a.everythingUnknown(null).foo()

    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.mixed(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>.foo()
    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.mixed(x)<!>.foo()

    a.explicitlyNullnessUnspecified(x).foo()
    a.explicitlyNullnessUnspecified(null).foo()

    a.defaultField.foo()

    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.foo()
}