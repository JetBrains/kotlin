// RENDER_DIAGNOSTICS_FULL_TEXT
// !LANGUAGE: +ForbidInferringTypeVariablesIntoEmptyIntersection
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>): S = null <!UNCHECKED_CAST!>as S<!>
}

fun <K : L, L : N, N: <!FINAL_UPPER_BOUND!>Int<!>> main() {
    val foo = Foo<K>()
    val x: Float = Bar<String>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR("S; kotlin.String, K; multiple incompatible classes; : kotlin/String, kotlin/Int")!>takeFoo<!>(foo) // error in 1.3.72, no error in 1.4.31
}
