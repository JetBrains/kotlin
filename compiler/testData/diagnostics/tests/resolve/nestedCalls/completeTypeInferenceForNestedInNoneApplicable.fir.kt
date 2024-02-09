package h

fun foo(i: Int) = i
fun foo(s: String) = s

fun test() {
    <!NONE_APPLICABLE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>emptyList<!>())
}

fun <T> emptyList(): List<T> {throw Exception()}
