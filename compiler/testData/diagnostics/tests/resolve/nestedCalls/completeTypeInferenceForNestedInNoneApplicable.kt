// !WITH_NEW_INFERENCE
package h

fun foo(i: Int) = i
fun foo(s: String) = s

fun test() {
    <!NONE_APPLICABLE!>foo<!>(<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>())
}

fun <T> emptyList(): List<T> {throw Exception()}