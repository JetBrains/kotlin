// WITH_STDLIB

// FILE: lib.kt
open class A<T1>()
class B<T2>() : A<T2>()

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T: A<*>> foo(f: Any?): Boolean {
    return f is T?
}

// FILE: main.kt
import kotlin.test.*

fun bar(): Boolean {
    return foo<B<Int>>(B<Int>())
}

fun box(): String {
    assertTrue(bar())
    return "OK"
}
