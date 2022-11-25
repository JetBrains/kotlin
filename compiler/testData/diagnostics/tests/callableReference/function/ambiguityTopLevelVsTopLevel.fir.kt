// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE
fun foo(x: Int, y: Any) = x
fun foo(x: Any, y: Int) = y

fun main() {
    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>

    val fooRef: (Int, Any) -> Unit = ::foo
}
