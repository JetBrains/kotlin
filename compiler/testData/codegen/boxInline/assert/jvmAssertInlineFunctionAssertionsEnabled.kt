// IGNORE_BACKEND: JVM_IR
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
        throw RuntimeException("FAIL 0")
    }
}

class Dummy

fun enableAssertions(): Checker {
    val loader = Dummy::class.java.classLoader
    loader.setDefaultAssertionStatus(true)
    val c = loader.loadClass("Checker")
    return c.newInstance() as Checker
}

fun box(): String {
    var c = enableAssertions()
    try {
        c.check()
        return "FAIL 2"
    } catch (ignore: AssertionError) {}

    return "OK"
}