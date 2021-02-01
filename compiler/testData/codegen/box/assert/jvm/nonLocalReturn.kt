// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// WITH_RUNTIME

package nonLocalReturn

interface Checker {
    fun checkTrueWithMessage(): Boolean
    fun checkFalseWithMessage(): Boolean
}

class ShouldBeDisabled : Checker {
    override fun checkTrueWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; true }

        assert(l()) {
            throw RuntimeException("FAIL 1")
        }

        return hit
    }

    override fun checkFalseWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; false }

        assert(l()) {
            throw RuntimeException("FAIL 3")
        }

        return hit
    }
}

class ShouldBeEnabled : Checker {
    override fun checkTrueWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; true }

        assert(l()) {
            throw RuntimeException("FAIL 5")
        }

        return hit
    }

    override fun checkFalseWithMessage(): Boolean {
        var hit = false
        val l = { hit = true; false }

        assert(l()) {
            return hit
            "BOOYA"
        }

        throw RuntimeException("FAIL 7")
    }
}

fun setDesiredAssertionStatus(v: Boolean): Checker {
    val loader = Checker::class.java.classLoader
    loader.setPackageAssertionStatus("nonLocalReturn", v)
    val c = loader.loadClass(if (v) "nonLocalReturn.ShouldBeEnabled" else "nonLocalReturn.ShouldBeDisabled")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = setDesiredAssertionStatus(false)
    if (c.checkTrueWithMessage()) return "FAIL 2"
    if (c.checkFalseWithMessage()) return "FAIL 4"
    c = setDesiredAssertionStatus(true)
    if (!c.checkTrueWithMessage()) return "FAIL 6"
    if (!c.checkFalseWithMessage()) return "FAIL 8"

    return "OK"
}
