// WITH_STDLIB

import kotlin.test.*

// vtable call + interface call
interface Z {
    fun foo(): Any
}

interface Y {
    fun foo(): Int
}

open class A {
    fun foo(): Int = 42
}

open class B: A(), Z, Y

fun box(): String {
    val z: Z = B()
    val y: Y = z as Y
    val res1 = z.foo().toString()
    if (res1 != "42") return "FAIL 1: $res1"
    val res2 = y.foo().toString()
    if (res2 != "42") return "FAIL 2: $res2"

    return "OK"
}
