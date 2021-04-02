interface A {
    fun <E> foo(a: E)
}

interface B {
    fun <T> foo(b: T)
}

interface C : A, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(a = 1)
    c.foo(<!NAMED_PARAMETER_NOT_FOUND!>b<!> = 1<!NO_VALUE_FOR_PARAMETER!>)<!>
}
