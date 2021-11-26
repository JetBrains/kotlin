class A<_> {}

typealias Foo<K, _> = Foo<K>

fun <K : T, _, T> foo() {}

fun <K, _: K> foo() {}
