interface P<U, Y>

class A<T> {
    class B {
        fun test() {
            class C<W>() : P<W, <!UNRESOLVED_REFERENCE!>T<!>> {
                <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object : P<<!UNRESOLVED_REFERENCE!>W<!>, <!UNRESOLVED_REFERENCE!>T<!>> {
                }

                inner class D : P<W, <!UNRESOLVED_REFERENCE!>T<!>>
            }
        }
    }
}