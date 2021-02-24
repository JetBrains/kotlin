// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// WITH_RUNTIME

// If assertions are disabled, neither argument to assert should be evaluated.
// If assertions are enabled, both arguments should be evaluate to values before
// checking the assertion.

package assertions

interface Checker {
    fun check(): Boolean
}

class Checker1 : Checker {
    override fun check(): Boolean {
        var result = true
        assert(true, {
            result = false
            { "Assertion failure" }
        }())
        return result
    }
}

class Checker2 : Checker {
    override fun check(): Boolean {
        var result = true
        assert(true, {
            result = false
            { "Assertion failure" }
        }())
        return result
    }
}

fun checkerWithAssertions(enabled: Boolean): Checker {
    val loader = Checker::class.java.classLoader
    loader.setPackageAssertionStatus("assertions", enabled)
    val c = loader.loadClass(if (enabled) "assertions.Checker1" else "assertions.Checker2")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = checkerWithAssertions(true)
    if (c.check()) return "Fail 1"
    c = checkerWithAssertions(false)
    if (!c.check()) return "Fail 2"
    return "OK"
}
