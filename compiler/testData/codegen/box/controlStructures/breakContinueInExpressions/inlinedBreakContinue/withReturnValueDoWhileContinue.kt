// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM

inline fun foo(block: () -> Int): Int  = block()

fun box(): String {
    var sum = 0
    var i = 1
    do {
        sum += foo { if (i == 3) continue else i }
    } while (++i <= 5)

    return if (sum == 1 + 2 + 4 + 5) "OK" else "FAIL: $sum"
}