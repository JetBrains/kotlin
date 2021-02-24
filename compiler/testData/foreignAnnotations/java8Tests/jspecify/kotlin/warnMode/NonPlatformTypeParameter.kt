// JAVA_SOURCES: NonPlatformTypeParameter.java
// JSPECIFY_STATE warn

fun <T : Test> main(a1: NonPlatformTypeParameter<Any?>, a2: NonPlatformTypeParameter<Test>, x: T): Unit {
    a1.foo(null)
    a1.bar<Test?>(null)
    // jspecify_nullness_mismatch
    a1.bar<T>(null)
    a1.bar<T>(x)

    // jspecify_nullness_mismatch
    a2.foo(null)
    a2.bar<Test?>(null)
    // jspecify_nullness_mismatch
    a2.bar<T>(null)
    a2.bar<T>(x)
}