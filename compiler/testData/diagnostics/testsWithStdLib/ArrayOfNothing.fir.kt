// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNCHECKED_CAST -USELESS_CAST
// !LANGUAGE: +ProhibitNonReifiedArraysAsReifiedTypeArguments
class A<T>

fun test1(
    a: Array<Nothing>,
    b: Array<Nothing?>,
    c: Array<in Nothing>,
    d: Array<in Nothing?>,
    e: Array<out Nothing>,
    f: Array<out Nothing?>
) {}

fun test2(
    a: Array<Nothing>?,
    b: Array<Nothing?>?,
    c: Array<in Nothing>?,
    d: Array<in Nothing?>?,
    e: Array<out Nothing>?,
    f: Array<out Nothing?>?
) {}

fun test3(
    a: A<Array<Nothing>>,
    b: A<Array<Nothing?>>,
    c: A<Array<in Nothing>>,
    d: A<Array<in Nothing?>>,
    e: A<Array<out Nothing>>,
    f: A<Array<out Nothing?>>
) {}

fun test4(
    a: Array<A<Nothing>>,
    b: Array<A<Nothing?>>,
    c: Array<A<in Nothing>>,
    d: Array<A<in Nothing?>>,
    e: Array<A<out Nothing>>,
    f: Array<A<out Nothing?>>
) {}

fun test5() {
    arrayOf<Nothing>()
    Array<Nothing>(10) { throw Exception() }
}

fun <T> foo(): Array<T> = (object {} as Any) as Array<T>

fun test6() = foo<Nothing>()


class B<T>(val array: Array<T>)

fun <T> bar() = B<Array<T>>(arrayOf())

fun test7() = bar<Nothing>()
