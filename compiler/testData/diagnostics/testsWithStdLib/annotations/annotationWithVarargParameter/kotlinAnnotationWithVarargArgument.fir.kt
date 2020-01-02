// !WITH_NEW_INFERENCE

annotation class B(vararg val args: String)

@B(*arrayOf(1, "b"))
fun test() {
}
