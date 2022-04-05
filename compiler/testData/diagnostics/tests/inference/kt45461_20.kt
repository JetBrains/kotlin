// FIR_IDENTICAL
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class In<in P>
class Inv<O>

fun <K : In<L>, L : N, N: Inv<Number>> main() {
    val foo = Foo<K>()
    Bar<In<Inv<Int>>>().takeFoo(foo) // error in 1.3.72, no error in 1.4.31
}
