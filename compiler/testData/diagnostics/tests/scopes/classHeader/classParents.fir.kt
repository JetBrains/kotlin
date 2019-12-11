// !DIAGNOSTICS: -UNUSED_PARAMETER

interface I<F, G, H>

class A(impl: Interface) : Nested(), Interface by impl, Inner, I<Nested, Interface, Inner> {

    class Nested

    inner class Inner

    interface Interface
}
