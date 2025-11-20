// WITH_STDLIB

import kotlin.test.*

class B(val a:Int, b:Int) {
    constructor(pos:Int):this(1, pos) {}
    val pos = b + 1
}

fun primaryConstructorCall(a:Int, b:Int) = B(a, b).pos

fun secondaryConstructorCall(a:Int) = B(a).pos

fun box(): String {
    assertEquals(42, primaryConstructorCall(0xdeadbeef.toInt(), 41))
    assertEquals(42, secondaryConstructorCall(41))

    return "OK"

}
