// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE
fun foo(x: Int, <!UNUSED_PARAMETER!>y<!>: Any) = x
fun foo(<!UNUSED_PARAMETER!>x<!>: Any, y: Int) = y

fun main() {
    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    
    val fooRef: (Int, Any) -> Unit = ::<!NONE_APPLICABLE!>foo<!>
}
