interface A {
    fun foo(a1: Int, a2: Double)
}

interface B {
    fun foo(b1: Int, b2: Double)
}

interface C : A, B { // Warning here, this is correct, C.foo has no named parameters
}

interface D : C {
    override fun foo(d1: Int, d2: Double)
}

fun test1(d: D) {
    d.foo(d1 = 1, d2 = 1.0)
}

fun test2(c: C) {
    c.foo(<!NAMED_PARAMETER_NOT_FOUND!>b1<!> = 1, <!NAMED_PARAMETER_NOT_FOUND!>b2<!> = 1.0<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>)<!>
}
