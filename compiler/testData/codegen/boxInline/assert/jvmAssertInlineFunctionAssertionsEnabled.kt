// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: inline.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME
// FULL_JDK

package test

inline fun inlineMe() {
    assert(false) { "FROM INLINED" }
}

// FILE: inlineSite.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm

import test.*

class CheckerJvmAssertInlineFunctionAssertionsEnabled {
    fun check() {
        inlineMe()
        throw RuntimeException("FAIL 0")
    }
}

class Dummy

fun enableAssertions(): CheckerJvmAssertInlineFunctionAssertionsEnabled {
    val loader = Dummy::class.java.classLoader
    loader.setClassAssertionStatus("CheckerJvmAssertInlineFunctionAssertionsEnabled", true)
    val c = loader.loadClass("CheckerJvmAssertInlineFunctionAssertionsEnabled")
    return c.newInstance() as CheckerJvmAssertInlineFunctionAssertionsEnabled
}

fun box(): String {
    var c = enableAssertions()
    try {
        c.check()
        return "FAIL 2"
    } catch (ignore: AssertionError) {}
    return "OK"
}