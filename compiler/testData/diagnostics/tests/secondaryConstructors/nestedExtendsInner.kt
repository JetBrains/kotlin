// RUN_PIPELINE_TILL: FRONTEND
class A {
    open inner class Inner

    class Nested : Inner {
        <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>constructor()<!>
    }
}
