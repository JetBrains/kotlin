// SKIP_WHEN_OUT_OF_CONTENT_ROOT

// FILE: anno.kt

@Target(AnnotationTarget.TYPE)
annotation class Anno

// FILE: first.kt

class Foo<T>(val value: T)

fun test(foo: Foo<@Anno String>) {
    <expr>val x = foo.value</expr>
}