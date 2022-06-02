// !DIAGNOSTICS: -UNUSED_PARAMETER

fun bar(y: (Int) -> Int) = 1
fun foo(x: Float) = 10f
fun foo(x: String) = ""

fun main() {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(::<!UNRESOLVED_REFERENCE!>foo<!>) // no report about unresolved callable reference for `foo`
}
