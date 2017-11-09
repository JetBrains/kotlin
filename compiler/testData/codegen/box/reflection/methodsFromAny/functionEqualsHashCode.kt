// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.*

fun top() = 42

fun Int.intExt(): Int = this

class A {
    fun mem() {}
}

class B {
    fun mem() {}
}


fun checkEqual(x: Any, y: Any) {
    assertEquals(x, y)
    assertEquals(x.hashCode(), y.hashCode(), "Elements are equal but their hash codes are not: ${x.hashCode()} != ${y.hashCode()}")
}

fun box(): String {
    checkEqual(::top, ::top)
    checkEqual(Int::intExt, Int::intExt)
    checkEqual(A::mem, A::mem)

    assertFalse(::top == Int::intExt)
    assertFalse(::top == A::mem)
    assertFalse(A::mem == B::mem)

    return "OK"
}
