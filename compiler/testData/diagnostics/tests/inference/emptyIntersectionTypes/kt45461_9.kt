// FIR_IDENTICAL
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Inv<P>

fun <K : Inv<L>, L : N, N: Number> main() {
    val foo = Foo<Inv<Number>>()
    Bar<Inv<Int>>().takeFoo(foo) // error in 1.3.72, no error in 1.4.31
}
