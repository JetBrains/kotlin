// !WITH_NEW_INFERENCE
//KT-2283 Bad diagnostics of failed type inference
package a


interface Foo<A>

fun <A, B> Foo<A>.map(f: (A) -> B): Foo<B> = object : Foo<B> {}


fun foo() {
    val l: Foo<String> = object : Foo<String> {}
    val m: Foo<String> = l.map { ppp -> <!ARGUMENT_TYPE_MISMATCH!>1<!> }
}
