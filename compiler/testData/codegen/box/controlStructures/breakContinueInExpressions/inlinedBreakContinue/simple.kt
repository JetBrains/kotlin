// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: ANDROID

// FILE: lib.kt
inline fun foo(block: () -> Unit) { block() }

// FILE: main.kt
fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}
