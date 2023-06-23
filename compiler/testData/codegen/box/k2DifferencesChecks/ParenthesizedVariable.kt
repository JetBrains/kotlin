// ORIGINAL: /compiler/testData/diagnostics/tests/deparenthesize/ParenthesizedVariable.fir.kt
// WITH_STDLIB
fun test() {
    (d@ val bar = 2)
}

fun box() = "OK".also { test() }
