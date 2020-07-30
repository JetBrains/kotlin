class A {
    companion object {

    }

    companion <!MANY_COMPANION_OBJECTS!>object<!> {

    }
}

class B {
    companion object A {

    }

    companion <!MANY_COMPANION_OBJECTS!>object B<!> {

    }
}