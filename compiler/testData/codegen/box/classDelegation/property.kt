// WITH_STDLIB

import kotlin.test.*

interface A {
    val x: Int
}

class C: A {
    override val x: Int = 42
}

class Q(a: A): A by a

fun box(): String {
    val q = Q(C())
    val a: A = q
    if (q.x != 42) return "FAIL q.x=${q.x}"
    if (a.x != 42) return "FAIL a.x=${a.x}"
    return "OK"
}
