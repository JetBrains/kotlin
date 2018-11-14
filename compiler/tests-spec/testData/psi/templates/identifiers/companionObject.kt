class A {
    companion object <!ELEMENT!> {

    }
}

class B {
    companion object <!ELEMENT!>: A() {

    }
}

class C {
    companion object <!ELEMENT!>: B by expr, x1, A() {

    }
}
