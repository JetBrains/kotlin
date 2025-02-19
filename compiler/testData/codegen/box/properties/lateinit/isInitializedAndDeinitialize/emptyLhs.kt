// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

package test

class Foo {
    lateinit var p: String

    fun test(): Boolean {
        if (!::p.isInitialized) {
            p = "OK"
            return false
        }
        return true
    }
}

object Bar {
    lateinit var p: String

    fun test(): Boolean {
        if (!::p.isInitialized) {
            p = "OK"
            return false
        }
        return true
    }
}

fun box(): String {
    val foo = Foo()
    if (foo.test()) return "Fail 1"
    if (!foo.test()) return "Fail 2"

    val bar = Bar
    if (bar.test()) return "Fail 3"
    if (!bar.test()) return "Fail 4"

    return bar.p
}
