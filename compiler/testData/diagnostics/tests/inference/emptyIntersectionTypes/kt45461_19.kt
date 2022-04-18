// RENDER_DIAGNOSTICS_FULL_TEXT
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Out<out P>
class Inv<O>

fun <K : Out<L>, L : N, N: Inv<Number>> main() {
    val foo = Foo<K>()
    Bar<Out<Inv<Int>>>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>takeFoo<!>(foo) // error in 1.3.72, no error in 1.4.31
}
