// WITH_STDLIB

import kotlin.test.*

inline class A(val x: Int)
inline class B(val a: A)
inline class C(val s: String)
inline class D(val c: C)

fun box(): String {
    val a = A(42)
    val b = B(a)
    val c = C("zzz")
    val d = D(c)
    assertTrue(a.equals(a))
    assertTrue(b.equals(b))
    assertTrue(c.equals(c))
    assertTrue(d.equals(d))

    return "OK"
}
