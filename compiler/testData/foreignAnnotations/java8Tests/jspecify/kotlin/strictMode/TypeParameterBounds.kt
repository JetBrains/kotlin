// JAVA_SOURCES: TypeParameterBounds.java

fun <T : Test> main(a1: A<Any?>, a2: A<Test>, b1: B<Any?>, b2: B<Test>, x: T): Unit {
    // jspecify_nullness_mismatch
    a1.foo(null)
    // jspecify_nullness_mismatch
    a1.bar<T?>(null)
    a1.bar<T>(x)

    // jspecify_nullness_mismatch
    a2.foo(null)
    // jspecify_nullness_mismatch
    a2.bar<T?>(null)
    a2.bar<T>(x)

    // jspecify_nullness_mismatch
    b1.foo(null)
    // jspecify_nullness_mismatch
    b1.bar<T?>(null)
    b1.bar<T>(x)

    // jspecify_nullness_mismatch
    b2.foo(null)
    // jspecify_nullness_mismatch
    b2.bar<T?>(null)
    b2.bar<T>(x)
}