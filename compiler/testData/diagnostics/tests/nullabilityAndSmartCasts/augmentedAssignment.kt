// FIR_IDENTICAL
class A {
    operator fun plusAssign(s: String) {}
}

fun test() {
    var a: A? = A()
    a <!UNSAFE_OPERATOR_CALL!>+=<!> ""
}

class B {
    operator fun plus(other: B) = this
}

fun test2() {
    var b: B? = B()
    b <!UNSAFE_OPERATOR_CALL!>+=<!> B()
}
