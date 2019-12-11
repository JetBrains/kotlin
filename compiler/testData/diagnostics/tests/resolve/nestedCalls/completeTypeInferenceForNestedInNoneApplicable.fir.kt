// !WITH_NEW_INFERENCE
package h

fun foo(i: Int) = i
fun foo(s: String) = s

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(emptyList())
}

fun <T> emptyList(): List<T> {throw Exception()}