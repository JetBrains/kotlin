// IS_APPLICABLE: false
// ERROR: Cannot infer a type for this parameter. Please specify it explicitly.
// ERROR: Cannot infer a type for this parameter. Please specify it explicitly.
fun main() {
    val sum = { (x, <caret>y) -> x + y  // Type of x and y cannot be inferred, so intention can't be used
}
