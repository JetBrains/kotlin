// !WITH_NEW_INFERENCE

annotation class B(vararg val args: String)

@B(*<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH("Array<out String>", "IGNORE")!>arrayOf(1, "b")<!>)
fun test() {
}
