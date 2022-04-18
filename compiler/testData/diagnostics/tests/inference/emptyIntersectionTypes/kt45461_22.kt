// FIR_IDENTICAL
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Inv<O>

fun <K : Inv<out Inv<out Number>>> main() {
    val foo = Foo<K>()
    Bar<Inv<Inv<Int>>>().takeFoo(foo) // error in 1.3.72, no error in 1.4.31
}
