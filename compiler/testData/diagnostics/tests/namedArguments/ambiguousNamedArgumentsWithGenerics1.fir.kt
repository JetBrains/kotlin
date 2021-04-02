interface A<T> {
    fun foo(a: T)
}

interface B {
    fun foo(b: Int)
}

interface C : A<Int>, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(a = 1)
    c.foo(<!NAMED_PARAMETER_NOT_FOUND!>b<!> = 1<!NO_VALUE_FOR_PARAMETER!>)<!>
}
