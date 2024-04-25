// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

interface A<T: A<T>>
interface C<T> : A<C<T>>
interface D<T> : A<C<T>>

fun <S> select(vararg args: S): S = TODO()

fun test(c: C<String>, d: D<String>) {
    val v = select(c, d)
    <!DEBUG_INFO_EXPRESSION_TYPE("A<C<kotlin.String>>")!>v<!>
}
