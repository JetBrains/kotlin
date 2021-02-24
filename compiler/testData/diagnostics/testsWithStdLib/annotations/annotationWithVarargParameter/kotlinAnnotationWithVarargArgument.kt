// !WITH_NEW_INFERENCE

annotation class B(vararg val args: String)

@B(*<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}!>arrayOf(<!CONSTANT_EXPECTED_TYPE_MISMATCH{NI}!>1<!>, "b")<!>)
fun test() {
}
