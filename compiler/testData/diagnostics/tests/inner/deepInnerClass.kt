interface P<U, Y>

class A<T> {
    class B {
        fun test() {
            class C<W>() : P<W, <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>T<!>> {
                <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object : P<<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>W<!>, <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>T<!>> {
                }

                inner class D : P<W, <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>T<!>>
            }
        }
    }
}