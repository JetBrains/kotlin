// FIX: Replace with `coerceAtMost` function
// WITH_RUNTIME
fun test(x: Double, y: Double) {
    <caret>Math.min(x, y)
}