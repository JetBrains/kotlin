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

fun test1(x: C<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.DA) = x
fun test2(x: C.SDA) = x
fun test3(x: C<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.TSDA) = x
fun test4(x: C<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><Int><!>.TC) = x

fun test5(x: C<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><*><!>.DA) = x
fun test6(x: C<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><*><!>.TSDA) = x
fun test7(x: C<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><*><!>.TC) = x

fun test8(x: C.SSDA) = x
fun test9(x: C.SSC) = x
