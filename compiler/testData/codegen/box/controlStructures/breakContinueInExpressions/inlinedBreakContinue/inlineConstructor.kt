// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: ANDROID
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
