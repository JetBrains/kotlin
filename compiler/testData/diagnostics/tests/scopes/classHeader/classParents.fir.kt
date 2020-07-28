// !DIAGNOSTICS: -UNUSED_PARAMETER

interface I<F, G, H>

class A(impl: Interface) : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE!>Interface<!> by impl, <!UNRESOLVED_REFERENCE!>Inner<!>, <!UNRESOLVED_REFERENCE!>I<Nested, Interface, Inner><!> {

    class Nested

    inner class Inner

    interface Interface
}
