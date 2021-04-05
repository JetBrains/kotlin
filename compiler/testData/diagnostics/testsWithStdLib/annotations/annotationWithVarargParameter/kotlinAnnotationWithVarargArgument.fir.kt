// !WITH_NEW_INFERENCE

annotation class B(vararg val args: String)

@B(*<!ARGUMENT_TYPE_MISMATCH!>arrayOf(1, "b")<!>)
fun test() {
}
