// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// FILE: inline.kt

package test

object CrossinlineLambdaContainer {
    inline fun call(crossinline c: () -> Boolean) {
        Runnable { assert(c()) }.run()
    }
}

// FILE: inlineSite.kt

import test.CrossinlineLambdaContainer.call

interface Checker {
    fun checkTrue(): Boolean
    fun checkFalse(): Boolean
}

class ShouldBeDisabled : Checker {
    override fun checkTrue(): Boolean {
        var hit = false
        call { hit = true; true }
        return hit
    }

    override fun checkFalse(): Boolean {
        var hit = false
        call { hit = true; false }
        return hit
    }
}

class ShouldBeEnabled : Checker {
    override fun checkTrue(): Boolean {
        var hit = false
        call { hit = true; true }
        return hit
    }

    override fun checkFalse(): Boolean {
        var hit = false
        call { hit = true; false }
        return hit
    }
}

fun setDesiredAssertionStatus(v: Boolean): Checker {
    val loader = Checker::class.java.classLoader
    loader.setDefaultAssertionStatus(v)
    val c = loader.loadClass(if (v) "ShouldBeEnabled" else "ShouldBeDisabled")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = setDesiredAssertionStatus(false)
    if (c.checkTrue()) return "FAIL 0"
    if (c.checkFalse()) return "FAIL 2"
    c = setDesiredAssertionStatus(true)
    if (!c.checkTrue()) return "FAIL 4"
    try {
        c.checkFalse()
        return "FAIL 6"
    } catch (ignore: AssertionError) {
    }
    return "OK"
}
