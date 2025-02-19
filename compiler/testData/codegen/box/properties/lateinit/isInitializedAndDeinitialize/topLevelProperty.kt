// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

lateinit var bar: String

fun box(): String {
    if (::bar.isInitialized) return "Fail 1"
    bar = "OK"
    if (!::bar.isInitialized) return "Fail 2"
    return bar
}
