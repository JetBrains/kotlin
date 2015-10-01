package o

class A {
    fun foo(b: B) = b
}

class B {
    fun bar() {}
}

fun test(a: A, b: B?) {
    a foo b!!
    <!DEBUG_INFO_SMARTCAST!>b<!>.bar()
}