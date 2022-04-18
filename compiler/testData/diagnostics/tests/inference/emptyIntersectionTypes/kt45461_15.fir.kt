// RENDER_DIAGNOSTICS_FULL_TEXT
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Inv<P>

interface A

fun <K : Inv<T>, T> main() where T: A, T: Number {
    val foo = Foo<K>()
    Bar<Inv<Int>>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION!>takeFoo<!>(foo) // error in 1.3.72, no error in 1.4.31
}