// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<F> {
    fun getSum(): F = TODO()
}

fun <S> select(vararg args: S): S = TODO()

class Bar<B : B> : Foo<B> {
    val v = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select(
        getSum(),
        42
    )<!>
}
