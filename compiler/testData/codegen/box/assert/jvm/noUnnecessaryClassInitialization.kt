// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME

// Reusing the $assertionsDisabled field in the Outer class might seem like a good idea,
// but it would result in an error in this case.
class Outer {
    companion object {
        init { error("") }
    }

    init { assert(true) }

    class Inner {
        init { assert(true) }
    }
}

fun box(): String {
    try {
        Outer.Inner()
    } catch (e: Throwable) {
        return "Fail"
    }
    return "OK"
}
