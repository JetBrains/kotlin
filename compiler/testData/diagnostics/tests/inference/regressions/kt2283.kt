// !WITH_NEW_INFERENCE
//KT-2283 Bad diagnostics of failed type inference
package a


interface Foo<A>

fun <A, B> Foo<A>.map(f: (A) -> B): Foo<B> = object : Foo<B> {}


fun foo() {
    val l: Foo<String> = object : Foo<String> {}
    val m: Foo<String> = l.<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}!>map { ppp -> <!CONSTANT_EXPECTED_TYPE_MISMATCH{NI}, CONSTANT_EXPECTED_TYPE_MISMATCH{NI}!>1<!> }<!>
}
