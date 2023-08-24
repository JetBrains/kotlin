// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class C<T> {
    inner class D

    typealias DA = D
    typealias SDA = C<Int>.D
    typealias TSDA = C<T>.D
    typealias TC = C<T>
    typealias SSDA = C<*>.D
    typealias SSC = C<*>
}

fun test1(x: C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>.DA) = x
fun test2(x: C.SDA) = x
fun test3(x: C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>.TSDA) = x
fun test4(x: C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>.TC) = x

fun test5(x: C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>.DA) = x
fun test6(x: C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>.TSDA) = x
fun test7(x: C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>.TC) = x

fun test8(x: C.SSDA) = x
fun test9(x: C.SSC) = x
