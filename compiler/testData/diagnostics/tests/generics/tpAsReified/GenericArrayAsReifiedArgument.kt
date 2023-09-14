// FIR_IDENTICAL
// !LANGUAGE: +ProhibitNonReifiedArraysAsReifiedTypeArguments
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T> foo() {}

typealias TA<T, R> = Array<R>

fun <T> bar() {
    foo<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>()
    foo<<!TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR!>Array<T><!>>()
    foo<<!TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR!>Array<Array<T>><!>>()
    foo<TA<T, String>>()
    foo<<!TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR!>TA<String, T><!>>()
    foo<TA<TA<String, T>, String>>()
    foo<<!TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR!>TA<String, TA<String, T>><!>>()
    foo<Array<Int>>()
    foo<Array<Array<Int>>>()
    foo<IntArray>()
    foo<List<T>>()
    foo<List<Array<T>>>()
}

fun test(x: Array<String>, y: Array<*>) {
    bar<Int>()
}
