fun foo(x: (String) -> Int) {}
fun foo(x: () -> Int) {}


fun bar(): Int = 1
fun bar(x: String): Int = 1

fun main() {
    <!AMBIGUITY!>foo<!>(<!UNRESOLVED_REFERENCE!>::bar<!>)
}
