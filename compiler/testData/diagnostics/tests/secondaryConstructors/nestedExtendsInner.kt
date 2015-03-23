class A {
    open inner class Inner

    class Nested : Inner {
        <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>constructor()<!>
    }
}
