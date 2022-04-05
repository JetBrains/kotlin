// FIR_IDENTICAL
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

fun <K : L, L : N, N: <!FINAL_UPPER_BOUND!>Int<!>> main() {
    val foo = Foo<K>()
    Bar<Number>().takeFoo(foo) // error in 1.3.72, no error in 1.4.31
}
