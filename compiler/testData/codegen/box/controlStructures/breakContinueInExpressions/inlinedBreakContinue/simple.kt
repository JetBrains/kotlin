// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND_K1: ANY
// Reason: break/continue in inline lambdas unsupported

inline fun foo(block: () -> Unit) { block() }

fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}