package codegen.inline.inline13

import kotlin.test.*

open class A<T1>()
class B<T2>() : A<T2>()

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T: A<*>> foo(f: Any?): Boolean {
    return f is T?
}

fun bar(): Boolean {
    return foo<B<Int>>(B<Int>())
}

@Test fun runTest() {
    println(bar().toString())
}
