// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND_K1: ANY
// Reason: break/continue in inline lambdas unsupported
// WITH_STDLIB

fun box(): String {
    while (true) {
        Array(5) { i ->
            if (i == 0) break
            return "Fail"
            i * 2
        }
    }
    return "OK"
}