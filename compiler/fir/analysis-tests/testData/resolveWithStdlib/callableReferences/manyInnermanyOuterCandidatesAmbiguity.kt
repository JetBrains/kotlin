fun foo(x: (String) -> Int) {}
fun foo(x: () -> Int) {}


fun bar(): Int = 1
fun bar(x: String): Int = 1

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>)
}
