class A {
    private class AInner
}

class B {
    fun foo(<!EXPOSED_PARAMETER_TYPE!>value: A.<!INVISIBLE_REFERENCE!>AInner<!><!>) {

    }
}
