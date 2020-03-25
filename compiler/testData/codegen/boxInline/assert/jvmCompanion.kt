// TARGET_BACKEND: JVM
// FILE: inline.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME
// FULL_JDK
package test

var result = "OK"

class State {

    companion object {
        inline fun inlineMe() {
            assert(false) { "FROM INLINED" }
        }
    }
}

// FILE: inlineSite.kt
import test.*

class CheckerJvmAssertInlineFunctionAssertionsEnabled {
    fun check() {
        State.inlineMe()
        throw RuntimeException("FAIL 0")
    }
}

class Dummy

fun enableAssertions(): CheckerJvmAssertInlineFunctionAssertionsEnabled {
    val loader = Dummy::class.java.classLoader
    loader.setDefaultAssertionStatus(true)
    val c = loader.loadClass("CheckerJvmAssertInlineFunctionAssertionsEnabled")
    return c.newInstance() as CheckerJvmAssertInlineFunctionAssertionsEnabled
}

fun box(): String {
    var c = enableAssertions()
    try {
        c.check()
        return "FAIL 2"
    } catch (ignore: AssertionError) {}
    return result
}
