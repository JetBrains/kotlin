class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Out<out P>

interface A<K>
interface B<T> : A<T>
interface C<T> : A<T>

fun <K : C<Int>> main() {
    val foo = Foo<K>()
    Bar<B<String>>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION!>takeFoo<!>(foo) // error in 1.3.72, no error in 1.4.31
}
