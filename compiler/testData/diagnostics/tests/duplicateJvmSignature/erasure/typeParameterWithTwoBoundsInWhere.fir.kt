// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo
interface Bar

fun <T> foo(x: T): T where T: Foo, T: Bar {null!!}
fun foo(x: Foo): Foo {null!!}
fun foo(x: Bar): Bar {null!!}