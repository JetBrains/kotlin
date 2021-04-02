interface A {
    fun foo(a1: Int, a2: Double)
    fun bar(a1: Int, a2: Double, a3: String)
    fun baz(a1: Int, a2: Double, a3: String, a4: Int, a5: String)
}

interface B {
    fun foo(b1: Int, b2: Double)
    fun bar(a1: Int, a2: Double, b3: String)
    fun baz(a1: Int, b2: Double, a3: String, b4: Int, a5: String)
}

interface C : A, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(<!NAMED_PARAMETER_NOT_FOUND!>b1<!> = 1, <!NAMED_PARAMETER_NOT_FOUND!>b2<!> = 1.0<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>)<!>
    c.foo(a1 = 1, <!NAMED_PARAMETER_NOT_FOUND!>b2<!> = 1.0<!NO_VALUE_FOR_PARAMETER!>)<!>
    c.foo(a1 = 1, a2 = 1.0)
    c.foo(a1 = 1, a2 = 1.0)
    c.bar(a1 = 1, a2 = 1.0, <!NAMED_PARAMETER_NOT_FOUND!>b3<!>= ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    c.baz(a1 = 1, <!NAMED_PARAMETER_NOT_FOUND!>b2<!> = 1.0, a3 = "", <!NAMED_PARAMETER_NOT_FOUND!>b4<!> = 2, a5 = ""<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>)<!>
    c.baz(a1 = 1, a2 = 1.0, a3 = "", <!NAMED_PARAMETER_NOT_FOUND!>b4<!> = 2, a5 = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
}
