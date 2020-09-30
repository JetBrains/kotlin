// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE
fun foo(x: Int, y: Any) = x
fun foo(x: Any, y: Int) = y

fun main() {
    <!UNRESOLVED_REFERENCE!>::foo<!>
    
    val fooRef: (Int, Any) -> Unit = <!UNRESOLVED_REFERENCE!>::foo<!>
}
