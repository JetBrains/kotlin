// FIR_IDENTICAL
// JSPECIFY_STATE: warn

// FILE: Simple.java
import org.jspecify.nullness.*;

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
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(x, null)<!>.foo()
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, x)<!>.foo()

    a.bar().foo()

    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.foo()
}