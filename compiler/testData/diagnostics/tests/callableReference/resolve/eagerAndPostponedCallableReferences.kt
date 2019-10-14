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

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>multiple<!>, ::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>multiple<!>)
}