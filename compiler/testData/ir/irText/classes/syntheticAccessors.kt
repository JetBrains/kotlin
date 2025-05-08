// FILE: A.kt
@file:Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")

class A {
    private fun foo() = Unit
    @PublishedApi internal inline fun bar() = foo()
}

private fun baz() = Unit
@PublishedApi internal inline fun qux() = baz()

// FILE: B.kt

@file:Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
package sample.sample

class A {
    private fun foo() = Unit
    @PublishedApi internal inline fun bar() = foo()
}

private fun baz() = Unit
@PublishedApi internal inline fun qux() = baz()

// FILE: main.kt
fun box(): String {
    A().bar()
    qux()
    sample.sample.A().bar()
    sample.sample.qux()
    return "OK"
}