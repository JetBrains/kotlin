// ISSUE: KT-59326

// Test that we don't have this exception in Kotlin/JS:
// Unhandled JavaScript exception: expected expression, got keyword 'default'

class C(default: String = "O", val result: String = default)

fun foo(default: String = "K", result: String = default) = result

fun box(): String {
    return C().result + foo()
}
