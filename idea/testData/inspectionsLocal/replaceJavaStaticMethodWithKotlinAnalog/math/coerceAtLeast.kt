// FIX: Replace with `coerceAtLeast` function
// WITH_RUNTIME
fun test(x: Double, y: Double) {
    Math.<caret>max(x, y)
}