// SKIP_WHEN_OUT_OF_CONTENT_ROOT

// FILE: anno.kt

@Target(AnnotationTarget.TYPE)
annotation class Anno

// FILE: first.kt

class Wrapper<T>(val value: T)
class Foo<T>(val wrapper: Wrapper<T>)

fun test(foo: Foo<@Anno String>) {
    <expr>val x = foo.wrapper.value</expr>
}