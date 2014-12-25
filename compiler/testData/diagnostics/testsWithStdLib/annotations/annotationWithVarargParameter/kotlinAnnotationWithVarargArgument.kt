annotation class B(vararg val args: String)

[B(*<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH(kotlin.Array<out kotlin.String>; IGNORE)!>array<!>(1, "b"))]
fun test() {
}
