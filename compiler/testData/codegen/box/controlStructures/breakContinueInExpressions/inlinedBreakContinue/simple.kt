// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM

inline fun foo(block: () -> Unit) { block() }

fun box(): String {
    while (true) {
        foo { break }
        return "FAIL"
    }

    return "OK"
}