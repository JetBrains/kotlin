class A {
    open inner class Inner

    class Nested : Inner {
        <!UNRESOLVED_REFERENCE!>constructor()<!>
    }
}
