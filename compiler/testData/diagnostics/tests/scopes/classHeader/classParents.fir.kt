// !DIAGNOSTICS: -UNUSED_PARAMETER

interface I<F, G, H>

class A(impl: Interface) : <!OTHER_ERROR!>Nested<!>(), <!OTHER_ERROR!>Interface<!> by impl, <!OTHER_ERROR!>Inner<!>, <!OTHER_ERROR!>I<Nested, Interface, Inner><!> {

    class Nested

    inner class Inner

    interface Interface
}
