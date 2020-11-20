// JAVA_SOURCES: NullnessUnspecifiedTypeParameter.java
// JSPECIFY_STATE strict

fun main(
    a1: NullnessUnspecifiedTypeParameter<Any>,
    // jspecify_nullness_mismatch
    a2: NullnessUnspecifiedTypeParameter<<!UPPER_BOUND_VIOLATED!>Any?<!>>, x: Test): Unit {
    // jspecify_nullness_mismatch
    a1.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.foo(1)

    // jspecify_nullness_mismatch
    a2.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.foo(1)

    // jspecify_nullness_mismatch
    a1.bar(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    a1.bar(x, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.bar(x, 1)

    // jspecify_nullness_mismatch
    a2.bar(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    // jspecify_nullness_mismatch
    a2.bar(x, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.bar(x, 1)
}