// FIX: Replace with `coerceAtMost` function
// WITH_RUNTIME
fun test(x: Double, y: Double) {
    Math.<caret>min(x, y)
}