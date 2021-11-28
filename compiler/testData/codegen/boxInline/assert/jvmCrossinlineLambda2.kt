// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// FILE: inline.kt

package test

object CrossinlineLambdaContainer {
    inline fun call(b: Boolean, crossinline c: () -> Unit) {
        val l = {
            assert(b) { "FROM INLINED" }
            c()
        }
        l()
    }
}

// FILE: inlineSite.kt

import test.CrossinlineLambdaContainer.call

interface Checker {
    fun checkTrueTrue(): Boolean
    fun checkTrueFalse(): Boolean
    fun checkFalseTrue(): Boolean
    fun checkFalseFalse(): Boolean
    fun checkTrueWithMessageTrue(): Boolean
    fun checkTrueWithMessageFalse(): Boolean
    fun checkFalseWithMessageTrue(): Boolean
    fun checkFalseWithMessageFalse(): Boolean
}

class ShouldBeDisabled : Checker {
    override fun checkTrueTrue(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(true) {
            assert(l())
        }
        return hit
    }

    override fun checkTrueFalse(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(false) {
            assert(l())
        }
        return hit
    }

    override fun checkFalseTrue(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(true) {
            assert(l())
        }
        return hit
    }

    override fun checkFalseFalse(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(false) {
            assert(l())
        }
        return hit
    }

    override fun checkTrueWithMessageTrue(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(true) {
            assert(l()) { "BOOYA" }
        }
        return hit
    }

    override fun checkTrueWithMessageFalse(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(false) {
            assert(l()) { "BOOYA" }
        }
        return hit
    }

    override fun checkFalseWithMessageTrue(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(true) {
            assert(l()) { "BOOYA" }
        }
        return hit
    }

    override fun checkFalseWithMessageFalse(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(false) {
            assert(l()) { "BOOYA" }
        }
        return hit
    }
}

class ShouldBeEnabled : Checker {
    override fun checkTrueTrue(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(true) {
            assert(l())
        }
        return hit
    }

    override fun checkTrueFalse(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(false) {
            assert(l())
        }
        return hit
    }

    override fun checkFalseTrue(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(true) {
            assert(l())
        }
        return hit
    }

    override fun checkFalseFalse(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(false) {
            assert(l())
        }
        return hit
    }

    override fun checkTrueWithMessageTrue(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(true) {
            assert(l()) { "BOOYA" }
        }
        return hit
    }

    override fun checkTrueWithMessageFalse(): Boolean {
        var hit = false
        val l = { hit = true; true }
        call(false) {
            assert(l()) { "BOOYA" }
        }
        return hit
    }

    override fun checkFalseWithMessageTrue(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(true) {
            assert(l()) { "BOOYA" }
        }
        return hit
    }

    override fun checkFalseWithMessageFalse(): Boolean {
        var hit = false
        val l = { hit = true; false }
        call(false) {
            assert(l()) { "BOOYA" }
        }
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
    if (c.checkTrueTrue()) return "FAIL 00"
    if (c.checkTrueFalse()) return "FAIL 01"
    if (c.checkTrueWithMessageTrue()) return "FAIL 10"
    if (c.checkTrueWithMessageFalse()) return "FAIL 11"
    if (c.checkFalseTrue()) return "FAIL 20"
    if (c.checkFalseFalse()) return "FAIL 21"
    if (c.checkFalseWithMessageTrue()) return "FAIL 30"
    if (c.checkFalseWithMessageFalse()) return "FAIL 31"

    c = setDesiredAssertionStatus(true)
    if (!c.checkTrueTrue()) return "FAIL 40"
    try {
        c.checkTrueFalse()
        return "FAIL 41"
    } catch (ignore: AssertionError) {
    }
    if (!c.checkTrueWithMessageTrue()) return "FAIL 50"
    try {
        c.checkTrueWithMessageFalse()
        return "FAIL 51"
    } catch (ignore: AssertionError) {
    }
    try {
        c.checkFalseTrue()
        return "FAIL 60"
    } catch (ignore: AssertionError) {
    }
    try {
        c.checkFalseFalse()
        return "FAIL 61"
    } catch (ignore: AssertionError) {
    }
    try {
        c.checkFalseWithMessageTrue()
        return "FAIL 70"
    } catch (ignore: AssertionError) {
    }
    try {
        c.checkFalseWithMessageFalse()
        return "FAIL 71"
    } catch (ignore: AssertionError) {
    }

    return "OK"
}
