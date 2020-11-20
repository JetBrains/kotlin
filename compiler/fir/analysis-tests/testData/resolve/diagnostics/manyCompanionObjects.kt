class A {
    companion <!REDECLARATION!>object<!> {

    }

    companion <!MANY_COMPANION_OBJECTS, REDECLARATION!>object<!> {

    }
}

class B {
    companion object A {

    }

    companion <!MANY_COMPANION_OBJECTS!>object B<!> {

    }
}