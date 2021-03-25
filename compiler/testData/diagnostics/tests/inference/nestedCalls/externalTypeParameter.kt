// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
class A<T> {
    fun <S> foo(s: S): S = s
    fun <U> bar(s: U): List<T> = null!!

    fun test() = foo(bar(""))
}
