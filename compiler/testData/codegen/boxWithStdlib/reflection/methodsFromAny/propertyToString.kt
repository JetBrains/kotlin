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
    assertToString("val top", ::top)
    assertToString("var top2", ::top2)
    assertToString("val kotlin.String.ext", String::ext)
    assertToString("var kotlin.IntRange?.ext2", IntRange::ext2)
    assertToString("val test.A.mem", A::mem)
    assertToString("var test.B.mem", B::mem)
    return "OK"
}
