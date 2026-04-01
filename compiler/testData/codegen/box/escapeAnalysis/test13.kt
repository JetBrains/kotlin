// WITH_STDLIB
import kotlin.test.*

class A {
    var f: A? = null
}

fun foo(a: A, k: Int): A {
    return if (k == 0) a else foo(a.f!!, k - 1)
}

fun box(): String {
    val a = A()
    val a2 = A()
    a.f = a2
    assertEquals(a2, foo(a, 1))
    return "OK"
}
