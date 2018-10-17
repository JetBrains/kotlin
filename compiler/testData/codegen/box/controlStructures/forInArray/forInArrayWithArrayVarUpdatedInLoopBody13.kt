// WITH_RUNTIME
// LANGUAGE_VERSION: 1.3

// In Kotlin 1.0, in a for-in-array loop, range expression is cached in
// a local variable unless it is already a local variable.
// This caused the following quirky behavior:
// if an array variable is updated in the loop body, it affects the loop
// execution (see https://youtrack.jetbrains.com/issue/KT-21354).
// Most likely it is a bug, however, fixing it right now is a breaking
// change requiring a proper deprecation loop.
// When the design decision is made, it might be required to update this
// test (e.g., by adding a proper LANGUAGE_VERSION directive).
// Note that JS back-end handles this case "correctly".

fun box(): String {
    var xs = intArrayOf(1, 2, 3)
    var sum = 0
    for (x in xs) {
        sum = sum * 10 + x
        xs = intArrayOf(4, 5)
    }
    return if (sum == 123) "OK" else "Fail: $sum"
}