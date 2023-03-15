// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect open class C() {
    open fun f(p: Int = 1) : String
    open fun f2(p1: Int = 1, p2: Int = 2) : String
    open fun ff(p1: Int, p2: Int = 2) : String
    open fun fff(p1: Int, p2: Int, p3: Int = 3) : String
    open fun fffx(p1: Int, p2: Int = 4, p3: Int = 5) : String
}

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.test.assertEquals

actual open class C {
    actual open fun f(p: Int) =  "f" + p
    actual open fun f2(p1: Int, p2: Int) = "f2" + p1 + "" + p2
    actual open fun ff(p1: Int, p2: Int) = "ff" + p1 + "" + p2
    actual open fun fff(p1: Int, p2: Int, p3: Int) = "fff" + p1 + "" + p2 + "" + p3
    actual open fun fffx(p1: Int, p2: Int, p3: Int) = "fffx" + p1 + "" + p2 + "" + p3
}

fun box(): String {

    assertEquals("f1", C().f())
    assertEquals("f212", C().f2())
    assertEquals("ff12", C().ff(1))
    assertEquals("fff123", C().fff(1, 2))
    assertEquals("fffx345", C().fffx(3))

    return "OK"
}