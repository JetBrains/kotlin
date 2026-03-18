// COMPILATION_ERRORS

fun err0() {
    a::b()
}

fun err1() {
    a.b::c()
}

fun err2() {
    A::
}

fun err3() {
    ::
}

fun err4() {
    ::x()
}

fun err5() {
    ::x()()
}

fun err6() {
    ::x(foo)
}

fun err7() {
    ::x(fun foo() {})
}

fun err8() {
    C::x(foo)
}

fun err9() {
    C::x(fun foo() {})
}

fun err10() {
    String::class()
}

fun err11() {
    String::class(foo)
}

fun err12() {
    String::class(fun foo() {})
}

fun typeArgumentsError() {
    ::a<b>
    ::a<b,c<*>>
    a::b<c>

    ::a<b>()
}
