// ISSUE: KT-61844
// FIR_DUMP

class Foo<I, out O>(
    private val transformer: (I) -> O,
) {
    fun <I, O> transform(foo: Foo<I, O>, bar: I) {
        foo.<!FUNCTION_EXPECTED!>transformer<!>(bar)
        foo.<!INVISIBLE_REFERENCE!>transformer<!>.invoke(bar)
    }
}
