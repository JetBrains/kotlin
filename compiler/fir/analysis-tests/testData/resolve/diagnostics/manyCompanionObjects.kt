class A {
    <!REDECLARATION{LT}!>companion <!REDECLARATION{PSI}!>object<!> {

    }<!>

    <!MANY_COMPANION_OBJECTS{LT}, REDECLARATION{LT}!>companion <!MANY_COMPANION_OBJECTS{PSI}, REDECLARATION{PSI}!>object<!> {

    }<!>
}

class B {
    companion object A {

    }

    <!MANY_COMPANION_OBJECTS{LT}!>companion <!MANY_COMPANION_OBJECTS{PSI}!>object B<!> {

    }<!>
}
