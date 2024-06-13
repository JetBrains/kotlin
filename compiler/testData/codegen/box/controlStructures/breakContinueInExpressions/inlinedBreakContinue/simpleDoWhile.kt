// LANGUAGE: +BreakContinueInInlineLambdas
// IGNORE_BACKEND: JVM

inline fun foo(block: () -> Unit) { block() }

fun box(): String {
    var i = 0
    do {
        if (++i == 1)
            foo { continue }

        if (i == 2)
            foo { break }
        return "FAIL 1: $i"
    } while (true)

    if (i != 2) return "FAIL 2: $i"

    return "OK"
}