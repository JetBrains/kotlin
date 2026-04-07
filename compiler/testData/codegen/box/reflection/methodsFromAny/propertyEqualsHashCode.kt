// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.3.0
// KT-85411: Supported in 2.4.0-Beta2
// WITH_REFLECT

import kotlin.test.*

val top = 42
var top2 = -23

val Int.intExt: Int get() = this
val Char.charExt: Int get() = this.toInt()

class A(var mem: String)
class B(var mem: String)


fun checkEqual(x: Any, y: Any) {
    assertEquals(x, y)
    assertEquals(x.hashCode(), y.hashCode(), "Elements are equal but their hash codes are not: ${x.hashCode()} != ${y.hashCode()}")
}

fun box(): String {
    checkEqual(::top, ::top)
    checkEqual(::top2, ::top2)
    checkEqual(Int::intExt, Int::intExt)
    checkEqual(A::mem, A::mem)

    assertFalse(::top == ::top2)
    assertFalse(Int::intExt == Char::charExt)
    assertFalse(A::mem == B::mem)

    return "OK"
}
