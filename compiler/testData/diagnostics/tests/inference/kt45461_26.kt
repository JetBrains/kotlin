// RENDER_DIAGNOSTICS_FULL_TEXT
// !LANGUAGE: +ForbidInferringTypeVariablesIntoEmptyIntersection
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>): S = null <!UNCHECKED_CAST!>as S<!>
}

class Out<out K>

fun <K : L, L : N, N: <!FINAL_UPPER_BOUND!>Out<Int><!>> main() {
    val foo = Foo<K>()
    val x: Out<Float> = Bar<Out<String>>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR("S; Out<String>, K, Out<Float>")!>takeFoo<!>(foo)
}
