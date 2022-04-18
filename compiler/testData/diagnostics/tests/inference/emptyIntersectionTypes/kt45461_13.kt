// FIR_IDENTICAL
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

interface A

class Out<out K>

fun <K : L, L : N, N> main() where N: Out<A> {
    val foo = Foo<K>()
    Bar<Out<String>>().takeFoo(foo) // error in 1.3.72, no error in 1.4.31
}
