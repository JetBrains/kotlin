// DIAGNOSTICS: -UNUSED_PARAMETER
// Issues: KT-36816

interface Parent<T>

class Foo<K>(x: K?): Parent<K> {}
class Bar<T>(x: T): Parent<T> {}

fun <R> select(vararg x: R) = x[0]

fun <S> main(x: S) {
    select(Foo(x), Bar(x))
}



inline fun <R> test(transform: () -> R) {}

class Inv<T>(x: T?) {}

fun <K> foo(x: K) {
    test { Inv(x) }
}
