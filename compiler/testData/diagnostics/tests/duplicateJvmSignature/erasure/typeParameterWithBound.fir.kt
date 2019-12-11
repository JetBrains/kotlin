// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo

fun <T: Foo> foo(x: T): T {null!!}
fun foo(x: Foo): Foo {null!!}