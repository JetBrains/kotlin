// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : <!UNRESOLVED_REFERENCE!>Nested<!>, F: <!UNRESOLVED_REFERENCE!>Inner<!>, G: <!UNRESOLVED_REFERENCE!>Interface<!>> {

    class Nested

    inner class Inner

    interface Interface
}

class B<T, F, G> where T : <!UNRESOLVED_REFERENCE!>Nested<!>, F: <!UNRESOLVED_REFERENCE!>Inner<!>, G: <!UNRESOLVED_REFERENCE!>Interface<!> {

    class Nested

    inner class Inner

    interface Interface
}

