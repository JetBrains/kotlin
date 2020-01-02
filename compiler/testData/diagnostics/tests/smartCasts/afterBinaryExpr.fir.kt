package o

class A {
    <!INAPPLICABLE_INFIX_MODIFIER!>infix fun foo(b: B) = b<!>
}

class B {
    fun bar() {}
}

fun test(a: A, b: B?) {
    a foo b!!
    b.bar()
}