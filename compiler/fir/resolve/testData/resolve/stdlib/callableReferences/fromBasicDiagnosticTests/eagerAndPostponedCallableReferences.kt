interface A
interface B

fun multiple(a: A) {}
fun multiple(b: B) {}

fun singleA(a: A) {}
fun singleB(a: B) {}

fun <T> foo(f: (T) -> Unit, g: (T) -> Unit): T = TODO()

fun test() {
    val a1 = foo(::singleA, ::multiple)

    val a2 = foo(::singleB, ::multiple)

    val a3 = foo(::multiple, ::singleA)

    val a4 = foo(::multiple, ::singleB)

    val a5 = foo(::singleA, ::singleA)

    val a6 = foo(::singleA, ::singleB)

    foo(<!UNRESOLVED_REFERENCE!>::multiple<!>, <!UNRESOLVED_REFERENCE!>::multiple<!>)
}
