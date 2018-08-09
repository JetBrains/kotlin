// FILE: inline.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME

inline fun inlineMe() {
    assert(false) { "FROM INLINED" }
}

// FILE: inlineSite.kt
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm

class Checker {
    fun check() {
        inlineMe()
        assert(false) { "FROM INLINESITE" }
    }
}

class Dummy

fun disableAssertions(): Checker {
    val loader = Dummy::class.java.classLoader
    loader.setDefaultAssertionStatus(false)
    val c = loader.loadClass("Checker")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = disableAssertions()
    c.check()

    return "OK"
}