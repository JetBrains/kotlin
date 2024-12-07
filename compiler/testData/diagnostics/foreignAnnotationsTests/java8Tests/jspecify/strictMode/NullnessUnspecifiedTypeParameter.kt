// FIR_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: NullnessUnspecifiedTypeParameter.java
import org.jspecify.annotations.*;

@NullMarked
public class NullnessUnspecifiedTypeParameter<T> {
    public void foo(T t) {}

    public void bar(Test s, T t) {} // t should not become not nullable
}

// FILE: Test.java
public class Test {}

// FILE: main.kt
// jspecify_nullness_mismatch
fun main(a1: NullnessUnspecifiedTypeParameter<Any>, a2: NullnessUnspecifiedTypeParameter<<!UPPER_BOUND_VIOLATED!>Any?<!>>, x: Test): Unit {
    // jspecify_nullness_mismatch
    a1.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.foo(1)

    a2.foo(null)
    a2.foo(1)

    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a1.bar(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    a1.bar(x, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.bar(x, 1)

    // jspecify_nullness_mismatch
    a2.bar(<!NULL_FOR_NONNULL_TYPE!>null<!>, null)
    a2.bar(x, null)
    a2.bar(x, 1)
}
