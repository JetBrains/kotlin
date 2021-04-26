package a

fun test(c: C) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>c.b<!>)
}

fun foo(s: String) = s

class C(val b: Int) {}