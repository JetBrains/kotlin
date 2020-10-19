// JAVA_SOURCES: TypeParameterBounds.java
// JSPECIFY_STATE strict

fun <T : Test> main(a1: A<<!UPPER_BOUND_VIOLATED!>Any?<!>>, a2: A<Test>, b1: B<<!UPPER_BOUND_VIOLATED!>Any?<!>>, b2: B<Test>, x: T): Unit {
    // jspecify_nullness_mismatch
    a1.foo(null)
    // jspecify_nullness_mismatch
    a1.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.bar<T>(x)

    // jspecify_nullness_mismatch
    a2.foo(null)
    // jspecify_nullness_mismatch
    a2.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.bar<T>(x)

    // jspecify_nullness_mismatch
    b1.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    b1.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b1.bar<T>(x)

    // jspecify_nullness_mismatch
    b2.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    b2.bar<<!UPPER_BOUND_VIOLATED!>T?<!>>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    b2.bar<T>(x)
}