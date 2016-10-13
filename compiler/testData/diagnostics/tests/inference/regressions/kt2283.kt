//KT-2283 Bad diagnostics of failed type inference
package a


interface Foo<A>

fun <A, B> Foo<A>.map(<!UNUSED_PARAMETER!>f<!>: (A) -> B): Foo<B> = object : Foo<B> {}


fun foo() {
    val l: Foo<String> = object : Foo<String> {}
    val <!UNUSED_VARIABLE!>m<!>: Foo<String> = l.<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>map { <!UNUSED_PARAMETER!>ppp<!> -> 1 }<!>
}
