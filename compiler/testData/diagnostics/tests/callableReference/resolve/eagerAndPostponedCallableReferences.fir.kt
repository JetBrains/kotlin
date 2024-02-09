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
    a1

    val a2 = foo(::singleB, ::multiple)
    a2

    val a3 = foo(::multiple, ::singleA)
    a3

    val a4 = foo(::multiple, ::singleB)
    a4

    val a5 = foo(::singleA, ::singleA)
    a5

    val a6 = foo(::singleA, ::singleB)
    a6

    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>multiple<!>, ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>multiple<!>)
}
