// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: inline.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING

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
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm

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
    loader.setDefaultAssertionStatus(false)
    loader.setPackageAssertionStatus("test", v)
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
    if (c.checkTrueTrue()) return "FAIL 100"
    if (c.checkTrueFalse()) return "FAIL 101"
    if (c.checkTrueWithMessageTrue()) return "FAIL 110"
    if (c.checkTrueWithMessageFalse()) return "FAIL 111"
    if (c.checkFalseTrue()) return "FAIL 120"
    if (c.checkFalseFalse()) return "FAIL 121"
    if (c.checkFalseWithMessageTrue()) return "FAIL 130"
    if (c.checkFalseWithMessageFalse()) return "FAIL 131"

    return "OK"
}