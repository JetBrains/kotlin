// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// FILE: common.kt

expect open class C() {
    open fun f(p: Int = 2) : String
}

// FILE: platform.kt

import kotlin.test.assertEquals

actual open class C {
    actual open fun f(p: Int) = "C" + p
}

open class D : C() {
    override open fun f(p: Int) = "D" + p
}

fun box(): String {

    assertEquals("C2", C().f())
    assertEquals("C9", C().f(9))
    assertEquals("D2", D().f())
    assertEquals("D5", D().f(5))

    return "OK"
}