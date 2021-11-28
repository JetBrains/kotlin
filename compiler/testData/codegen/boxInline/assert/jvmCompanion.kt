// FULL_JDK
// WITH_STDLIB
// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// FILE: inline.kt
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
