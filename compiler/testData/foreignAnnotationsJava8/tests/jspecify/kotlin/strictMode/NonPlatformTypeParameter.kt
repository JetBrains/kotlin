// JAVA_SOURCES: NonPlatformTypeParameter.java
// JSPECIFY_STATE strict

fun <T : Test> main(a1: NonPlatformTypeParameter<Any?>, a2: NonPlatformTypeParameter<Test>, x: T): Unit {
    a1.foo(null)
    a1.bar<Test?>(null)
    // jspecify_nullness_mismatch
    a1.bar<T>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a1.bar<T>(x)

    // jspecify_nullness_mismatch
    a2.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.bar<Test?>(null)
    // jspecify_nullness_mismatch
    a2.bar<T>(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    a2.bar<T>(x)
}