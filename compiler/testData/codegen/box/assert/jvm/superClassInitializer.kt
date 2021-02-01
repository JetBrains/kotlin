// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// WITH_RUNTIME

package superClassInitializer

interface Checker {
    fun checkTrue(): Boolean
    fun checkFalse(): Boolean
    fun checkTrueWithMessage(): Boolean
    fun checkFalseWithMessage(): Boolean
}

open class IntHolder(i: Int)

class ShouldBeDisabled : Checker {
    override fun checkTrue(): Boolean {
        var hit = false
        val l = { hit = true; true }

        val local = object : IntHolder(run { assert(l()); 0 }) {}

        return hit
    }

    override fun checkFalse(): Boolean {
        var hit = false
        val l = { hit = true; false }

        val local = object : IntHolder(run { assert(l()); 0 }) {}

        return hit
    }

    override fun checkTrueWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; true }

        val local = object : IntHolder(run { assert(l()) { "BOOYA!" }; 0 }) {}

        return hit
    }

    override fun checkFalseWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; false }

        val local = object : IntHolder(run { assert(l()) { "BOOYA!" }; 0 }) {}

        return hit
    }
}

class ShouldBeEnabled : Checker {
    override fun checkTrue(): Boolean {
        var hit = false
        val l = { hit = true; true }

        val local = object : IntHolder(run { assert(l()); 0 }) {}

        return hit
    }

    override fun checkFalse(): Boolean {
        var hit = false
        val l = { hit = true; false }

        val local = object : IntHolder(run { assert(l()); 0 }) {}

        return hit
    }

    override fun checkTrueWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; true }

        val local = object : IntHolder(run { assert(l()) { "BOOYA!" }; 0 }) {}

        return hit
    }

    override fun checkFalseWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; false }

        val local = object : IntHolder(run { assert(l()) { "BOOYA!" }; 0 }) {}

        return hit
    }
}

fun setDesiredAssertionStatus(v: Boolean): Checker {
    val loader = Checker::class.java.classLoader
    loader.setPackageAssertionStatus("superClassInitializer", v)
    val c = loader.loadClass(if (v) "superClassInitializer.ShouldBeEnabled" else "superClassInitializer.ShouldBeDisabled")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = setDesiredAssertionStatus(false)
    if (c.checkTrue()) return "FAIL 0"
    if (c.checkTrueWithMessage()) return "FAIL 1"
    if (c.checkFalse()) return "FAIL 2"
    if (c.checkFalseWithMessage()) return "FAIL 3"
    c = setDesiredAssertionStatus(true)
    if (!c.checkTrue()) return "FAIL 4"
    if (!c.checkTrueWithMessage()) return "FAIL 5"
    try {
        c.checkFalse()
        return "FAIL 6"
    } catch (ignore: AssertionError) {
    }
    try {
        c.checkFalseWithMessage()
        return "FAIL 7"
    } catch (ignore: AssertionError) {
    }

    return "OK"
}
