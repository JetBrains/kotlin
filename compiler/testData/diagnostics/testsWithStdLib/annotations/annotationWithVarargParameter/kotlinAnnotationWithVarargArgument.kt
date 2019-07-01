// !WITH_NEW_INFERENCE

annotation class B(vararg val args: String)

@B(*<!NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>arrayOf(1, "b")<!>)
fun test() {
}
