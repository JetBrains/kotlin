// !LANGUAGE: -ProhibitNonReifiedArraysAsReifiedTypeArguments
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T> foo() {}

fun <T> bar() {
    foo<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>()
    foo<<!TYPE_PARAMETER_AS_REIFIED_ARRAY_WARNING!>Array<T><!>>()
    foo<<!TYPE_PARAMETER_AS_REIFIED_ARRAY_WARNING!>Array<Array<T>><!>>()
    foo<Array<Int>>()
    foo<Array<Array<Int>>>()
    foo<IntArray>()
    foo<List<T>>()
    foo<List<Array<T>>>()
}

fun test(x: Array<String>, y: Array<*>) {
    bar<Int>()
}
