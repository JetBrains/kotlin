// RENDER_DIAGNOSTICS_FULL_TEXT
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Inv<O>

fun <K : <!FINAL_UPPER_BOUND!>Inv<out Inv<out Int>><!>> main() {
    val foo = Foo<K>()
    Bar<Inv<in Inv<in Number>>>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION!>takeFoo<!>(foo) // error in 1.3.72, no error in 1.4.31
}
