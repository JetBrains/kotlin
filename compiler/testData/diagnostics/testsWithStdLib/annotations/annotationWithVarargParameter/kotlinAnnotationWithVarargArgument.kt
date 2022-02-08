annotation class B(vararg val args: String)

@B(*<!TYPE_MISMATCH!>arrayOf(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, "b")<!>)
fun test() {
}
