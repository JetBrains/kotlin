// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object B

class A {
    operator fun B.rem(x: Int) = 0
}

fun test1() {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun B.mod(x: Int) = ""

    with(A()) {
        takeInt(B % 10)
    }
}

class C {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun B.mod(x: Int) = ""
}

fun test2() {
    operator fun B.rem(x: Int) = 0

    with(C()) {
        takeInt(B % 10)
    }
}

fun takeInt(x: Int) {}