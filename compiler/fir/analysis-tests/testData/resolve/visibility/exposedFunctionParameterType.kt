class A {
    private class AInner
}

class B {
    fun foo(<!EXPOSED_PARAMETER_TYPE!>value: <!INVISIBLE_REFERENCE!>A.AInner<!><!>) {

    }
}
