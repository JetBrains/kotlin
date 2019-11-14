// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions +SamConversionPerArgument +FunctionInterfaceConversion
// !CHECK_TYPE

fun interface F<S> {
    fun apply(s: S)
}

interface PR<X, Y> {}

interface K<T> {
    fun f_t(f1: F<T>, f2: F<T>)
    fun <R> f_r(f1: F<R>, f2: F<R>)
    fun <R> f_pr(f1: F<PR<T, R>>, f2: F<PR<T, R>>)
}

fun test(
    k: K<String>,
    f_string: F<String>,
    f_int: F<Int>,
    f_pr: F<PR<String, Int>>
) {
    k.f_t(f_string) { it checkType { _<String>() } }
    k.f_r(f_int) { it checkType { _<Int>() } }
    k.f_pr(f_pr) { it checkType { _<PR<String, Int>>() } }
}