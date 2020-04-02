// TARGET_BACKEND: JVM
// FILE: inline.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME
// FULL_JDK
package test

class A {
    inline fun a() {
        assert(false) { "from inlined" }
    }
}

class B {
    inline fun b() {
        A().a()
        error("FAIL 0")
    }
}

// FILE: inlineSite.kt
import test.*

class Checker {
    fun check() {
        B().b()
        error("FAIL 1")
    }
}

class Dummy

fun enableAssertions(): Checker {
    val loader = Dummy::class.java.classLoader
    loader.setDefaultAssertionStatus(true)
    val c = loader.loadClass("Checker")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = enableAssertions()
    try {
        c.check()
        return "FAIL 2"
    } catch (ignore: AssertionError) {}
    return "OK"
}
