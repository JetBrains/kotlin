// !DIAGNOSTICS: -UNUSED_EXPRESSION
fun foo(x: Int, <!UNUSED_PARAMETER!>y<!>: Any) = x
fun foo(<!UNUSED_PARAMETER!>x<!>: Any, y: Int) = y

fun main() {
    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    
    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> : (Int, Any) -> Unit
}