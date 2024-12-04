// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

class Inv<P>

interface A
interface B

fun <K : Inv<A>> main() {
    val foo = Foo<K>()
    Bar<Inv<B>>().takeFoo(foo) // error in 1.3.72, no error in 1.4.31
}
