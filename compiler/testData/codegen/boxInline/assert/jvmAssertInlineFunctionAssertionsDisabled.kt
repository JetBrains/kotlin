// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// FILE: inline.kt
// WITH_RUNTIME
// FULL_JDK
// TARGET_BACKEND: JVM

package test

inline fun inlineMe() {
    assert(false) { "FROM INLINED" }
}

// FILE: inlineSite.kt

import test.*

class CheckerJvmAssertInlineFunctionAssertionsDisabled {
    fun check() {
        inlineMe()
        assert(false) { "FROM INLINESITE" }
    }
}

class Dummy

fun disableAssertions(): CheckerJvmAssertInlineFunctionAssertionsDisabled {
    val loader = Dummy::class.java.classLoader
    loader.setClassAssertionStatus("CheckerJvmAssertInlineFunctionAssertionsDisabled", false)
    loader.setClassAssertionStatus("InlineKt", false)
    val c = loader.loadClass("CheckerJvmAssertInlineFunctionAssertionsDisabled")
    return c.newInstance() as CheckerJvmAssertInlineFunctionAssertionsDisabled
}

fun box(): String {
    var c = disableAssertions()
    c.check()
    return "OK"
}