// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<F> {
    fun getSum(): F = TODO()
}

fun <S> select(vararg args: S): S = TODO()

class Bar<B : <!CYCLIC_GENERIC_UPPER_BOUND!>B<!>> : Foo<B> {
    val v = <!DEBUG_INFO_EXPRESSION_TYPE("[ERROR : from type constructor([ERROR : Cyclic upper bounds])]")!>select(
        <!DEBUG_INFO_LEAKING_THIS!>getSum<!>(),
        42
    )<!>
}
