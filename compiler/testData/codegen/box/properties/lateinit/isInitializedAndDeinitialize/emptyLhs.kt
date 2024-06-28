// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB

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
