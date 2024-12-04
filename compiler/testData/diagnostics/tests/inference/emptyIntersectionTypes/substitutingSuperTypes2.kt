// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Out<out P>

interface A<K>
interface B2<T> : A<T>
interface C2<T> : A<T>
interface B<T> : B2<T>
interface C<T> : C2<T>

fun <K : C<Int>> main() {
    val foo = Foo<K>()
    Bar<B<String>>().takeFoo(foo) // error in 1.3.72, no error in 1.4.31
}
