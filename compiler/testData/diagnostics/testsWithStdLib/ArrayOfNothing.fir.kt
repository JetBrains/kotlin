// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNCHECKED_CAST -USELESS_CAST
// !LANGUAGE: +ProhibitNonReifiedArraysAsReifiedTypeArguments
class A<T>
class C<T, G>
class D<T>

fun test1(
    a: <!UNSUPPORTED!>Array<Nothing><!>,
    b: <!UNSUPPORTED!>Array<Nothing?><!>,
    c: <!UNSUPPORTED!>Array<in Nothing><!>,
    d: <!UNSUPPORTED!>Array<in Nothing?><!>,
    e: <!UNSUPPORTED!>Array<out Nothing><!>,
    f: <!UNSUPPORTED!>Array<out Nothing?><!>,
    g: C<String, <!UNSUPPORTED!>Array<Nothing><!>>,
    h: A<D<<!UNSUPPORTED!>Array<Nothing><!>>>
) {
    <!UNSUPPORTED!>A<!><D<<!UNSUPPORTED!>Array<Nothing><!>>>()
}

fun test2(
    a: <!UNSUPPORTED!>Array<Nothing>?<!>,
    b: <!UNSUPPORTED!>Array<Nothing?>?<!>,
    c: <!UNSUPPORTED!>Array<in Nothing>?<!>,
    d: <!UNSUPPORTED!>Array<in Nothing?>?<!>,
    e: <!UNSUPPORTED!>Array<out Nothing>?<!>,
    f: <!UNSUPPORTED!>Array<out Nothing?>?<!>
) {}

fun test3(
    a: A<<!UNSUPPORTED!>Array<Nothing><!>>,
    b: A<<!UNSUPPORTED!>Array<Nothing?><!>>,
    c: A<<!UNSUPPORTED!>Array<in Nothing><!>>,
    d: A<<!UNSUPPORTED!>Array<in Nothing?><!>>,
    e: A<<!UNSUPPORTED!>Array<out Nothing><!>>,
    f: A<<!UNSUPPORTED!>Array<out Nothing?><!>>
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
    <!UNSUPPORTED!>arrayOf<!><<!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>Nothing<!>>()
    <!UNSUPPORTED!>Array<!><<!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>Nothing<!>>(10) { throw Exception() }
}

fun <T> foo(): Array<T> = (object {} as Any) as Array<T>

fun test6() = <!UNSUPPORTED!>foo<!><Nothing>()

class B<T>(val array: Array<T>)

fun <T> bar() = B<Array<T>>(<!TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR!>arrayOf<!>())

fun test7() = <!UNSUPPORTED!>bar<!><Nothing>()
