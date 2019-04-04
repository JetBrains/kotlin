// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

interface A
interface B

fun multiple(a: A) {}
fun multiple(b: B) {}

fun singleA(a: A) {}
fun singleB(a: B) {}

fun <T> foo(f: (T) -> Unit, g: (T) -> Unit): T = TODO()

fun test() {
    val a1 = foo(::singleA, ::multiple)
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>a1<!>

    val a2 = foo(::singleB, ::multiple)
    <!DEBUG_INFO_EXPRESSION_TYPE("B")!>a2<!>

    val a3 = foo(::multiple, ::singleA)
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>a3<!>

    val a4 = foo(::multiple, ::singleB)
    <!DEBUG_INFO_EXPRESSION_TYPE("B")!>a4<!>

    val a5 = foo(::singleA, ::singleA)
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>a5<!>

    val a6 = foo(::singleA, ::singleB)
    <!DEBUG_INFO_EXPRESSION_TYPE("{A & B}")!>a6<!>

    <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>foo<!>(::<!DEBUG_INFO_MISSING_UNRESOLVED!>multiple<!>, ::<!DEBUG_INFO_MISSING_UNRESOLVED!>multiple<!>)
}