// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM

inline fun foo(block: () -> Int): Int  = block()

fun box(): String {
    var sum = 0

    for (i in 1..10) {
        sum += foo { if (i == 3) break else i }
    }

    return if (sum == 3) "OK" else "FAIL: $sum"
}