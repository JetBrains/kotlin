// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME

interface Checker {
    fun checkTrue(): Boolean {
        var hit = false
        val l = { hit = true; true }
        assert(l())
        return hit
    }

    fun checkFalse(): Boolean {
        var hit = false
        val l = { hit = true; false }
        assert(l())
        return hit
    }

    fun checkTrueWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; true }
        assert(l()) { "BOOYA" }
        return hit
    }

    fun checkFalseWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; false }
        assert(l()) { "BOOYA" }
        return hit
    }
}

class ShouldBeDisabled : Checker {}

class Dummy

fun disableAssertions(): Checker {
    val loader = Dummy::class.java.classLoader
    loader.setDefaultAssertionStatus(false)
    val c = loader.loadClass("ShouldBeDisabled")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = disableAssertions()
    if (c.checkTrue()) return "FAIL 0"
    if (c.checkTrueWithMessage()) return "FAIL 1"
    if (c.checkFalse()) return "FAIL 2"
    if (c.checkFalseWithMessage()) return "FAIL 3"

    return "OK"
}
