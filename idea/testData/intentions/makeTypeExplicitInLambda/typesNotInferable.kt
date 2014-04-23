// IS_APPLICABLE: false
// ERROR: Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) -> ...} notation
// ERROR: Cannot infer a type for this parameter. To specify it explicitly use the {(p : Type) -> ...} notation
fun main() {
    val sum = { (x, <caret>y) -> x + y  // Type of x and y cannot be inferred, so intention can't be used
}
