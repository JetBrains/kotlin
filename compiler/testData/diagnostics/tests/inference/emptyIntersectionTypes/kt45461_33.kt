class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Out<out P>

interface A<K>
class B : A<Int>
class C : A<String>

fun <K : <!FINAL_UPPER_BOUND!>Out<C><!>> main() {
    val foo = Foo<K>()
    Bar<Out<B>>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>takeFoo<!>(foo) // error in 1.3.72, no error in 1.4.31
}
