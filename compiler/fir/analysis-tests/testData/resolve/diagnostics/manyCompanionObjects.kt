class A {
    companion <!REDECLARATION!>object<!> {

    }

    <!MANY_COMPANION_OBJECTS!>companion<!> <!REDECLARATION!>object<!> {

    }
}

class B {
    companion object A {

    }

    <!MANY_COMPANION_OBJECTS!>companion<!> object B {

    }
}
