// !WITH_NEW_INFERENCE
//KT-2283 Bad diagnostics of failed type inference
package a


interface Foo<A>

fun <A, B> Foo<A>.map(<!UNUSED_PARAMETER!>f<!>: (A) -> B): Foo<B> = object : Foo<B> {}


fun foo() {
    val l: Foo<String> = object : Foo<String> {}
    val <!UNUSED_VARIABLE!>m<!>: Foo<String> = l.<!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>map { <!UNUSED_ANONYMOUS_PARAMETER!>ppp<!> -> <!NI;CONSTANT_EXPECTED_TYPE_MISMATCH, NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> }<!>
}
