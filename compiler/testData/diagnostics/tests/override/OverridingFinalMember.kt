// FIR_IDENTICAL
open class A {
    final fun foo() {}
}

class B : A() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun foo() {}
}
