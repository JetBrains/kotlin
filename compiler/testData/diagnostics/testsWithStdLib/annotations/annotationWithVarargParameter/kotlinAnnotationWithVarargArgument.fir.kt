// !WITH_NEW_INFERENCE

annotation class B(vararg val args: String)

<!INAPPLICABLE_CANDIDATE!>@B(*arrayOf(1, "b"))<!>
fun test() {
}
