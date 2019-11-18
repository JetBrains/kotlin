// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// FILE: common.kt

expect open class A() {
    open fun f(p: Int = 1) : String
}

expect open class B : A {
    override open fun f(p: Int) : String
}

// FILE: platform.kt

import kotlin.test.assertEquals

actual open class A {
    actual open fun f(p: Int) = "A" + p
}

actual open class B : A() {
    actual override open fun f(p: Int) = "B" + p
}

fun box(): String {

    assertEquals("A1", A().f())
    assertEquals("A9", A().f(9))
    assertEquals("B1", B().f())
    assertEquals("B5", B().f(5))

    return "OK"
}