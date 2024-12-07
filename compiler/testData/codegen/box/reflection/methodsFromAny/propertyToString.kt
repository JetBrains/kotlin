// TARGET_BACKEND: JVM

// WITH_REFLECT

package test

import kotlin.test.assertEquals

val top = 42
var top2 = -23

val String.ext: Int get() = 0
var IntRange?.ext2: Int get() = 0; set(value) {}

class A(val mem: String)
class B(var mem: String)

fun assertToString(s: String, x: Any) {
    assertEquals(s, x.toString())
}

fun box(): String {
    assertToString("val top: kotlin.Int", ::top)
    assertToString("var top2: kotlin.Int", ::top2)
    assertToString("val kotlin.String.ext: kotlin.Int", String::ext)
    assertToString("var kotlin.ranges.IntRange?.ext2: kotlin.Int", IntRange::ext2)
    assertToString("val test.A.mem: kotlin.String", A::mem)
    assertToString("var test.B.mem: kotlin.String", B::mem)
    assertToString("getter of val top: kotlin.Int", ::top.getter)
    assertToString("getter of var top2: kotlin.Int", ::top2.getter)
    assertToString("setter of var top2: kotlin.Int", ::top2.setter)
    assertToString("getter of val test.A.mem: kotlin.String", A::mem.getter)
    assertToString("getter of var test.B.mem: kotlin.String", B::mem.getter)
    assertToString("setter of var test.B.mem: kotlin.String", B::mem.setter)
    return "OK"
}
