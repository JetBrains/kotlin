// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-40396

val <C> C.foo get() = Foo<C>()

class Foo<K> {
    operator fun <T> invoke(body: () -> Unit) {}
}

class Bar {
    val bar = foo {}
    val baz = foo<Int> {}
}