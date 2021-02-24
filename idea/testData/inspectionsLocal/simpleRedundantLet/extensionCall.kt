// WITH_RUNTIME
fun String.foo() {}

fun test(s: String?) {
    s?.let<caret> { it.foo() }
}