// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

class Foo {
    lateinit var bar: String

    fun test(): String {
        var state = 0
        if (run { state++; this }::bar.isInitialized) return "Fail 1"

        bar = "A"
        if (!run { state++; this }::bar.isInitialized) return "Fail 3"

        return if (state == 2) "OK" else "Fail: state=$state"
    }
}

fun box(): String {
    return Foo().test()
}
