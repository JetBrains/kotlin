// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: +ForbidInferringTypeVariablesIntoEmptyIntersection
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>): S = null <!UNCHECKED_CAST!>as S<!>
}

fun <K : L, L : N, N: <!FINAL_UPPER_BOUND!>Int<!>> main() {
    val foo = Foo<K>()
    val x: Float = <!TYPE_MISMATCH!>Bar<String>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR("S; String, K; multiple incompatible classes; : String, Int"), TYPE_MISMATCH!>takeFoo<!>(foo)<!> // error in 1.3.72, no error in 1.4.31
}
