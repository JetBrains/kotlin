// ISSUE: KT-59326
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ KT-59326 is fixed in 2.2.20-Beta2

// Test that we don't have this exception in Kotlin/JS:
// Unhandled JavaScript exception: expected expression, got keyword 'default'

class C(default: String = "O", val result: String = default)

fun foo(default: String = "K", result: String = default) = result

fun box(): String {
    return C().result + foo()
}
