// FIR_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: A.java
import org.jspecify.annotations.*;

@NullMarked
public class A<T> {
    public void foo(@NullnessUnspecified T t) {}
    public <E> void bar(E e) {}
}

// FILE: B.java
import org.jspecify.annotations.*;
@NullMarked
public class B<T> {
    public void foo(T t) {}
    public <E> void bar(E e) {}
}

// FILE: Test.java
public class Test {}

// FILE: main.kt
// jspecify_nullness_mismatch, jspecify_nullness_mismatch
fun <T : Test> main(a1: A<<!UPPER_BOUND_VIOLATED!>Any?<!>>, a2: A<Test>, b1: B<<!UPPER_BOUND_VIOLATED!>Any?<!>>, b2: B<Test>, x: T): Unit {
    a1.foo(null)
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a1.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.bar<T>(x)

    a2.foo(null)
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a2.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.bar<T>(x)

    b1.foo(null)
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    b1.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b1.bar<T>(x)

    // jspecify_nullness_mismatch
    b2.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    b2.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b2.bar<T>(x)
}
