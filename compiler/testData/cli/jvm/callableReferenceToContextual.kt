context(s: String) fun foo() {}

fun String.test() {
    val ref: () -> Unit = ::foo
}
