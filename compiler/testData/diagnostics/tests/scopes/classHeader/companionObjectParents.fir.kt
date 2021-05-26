interface I<F, G>

val aImpl: A.Companion.Interface
    get() = null!!

val bImpl: B.Companion.Interface
    get() = null!!

interface A {
    companion object : <!CYCLIC_INHERITANCE_HIERARCHY!>Nested<!>(), <!CYCLIC_INHERITANCE_HIERARCHY!>Interface<!> by aImpl, <!CYCLIC_INHERITANCE_HIERARCHY!>I<Nested, Interface><!> {

        class Nested

        interface Interface
    }
}

class B {
    companion object : <!CYCLIC_INHERITANCE_HIERARCHY!>Nested<!>(), <!CYCLIC_INHERITANCE_HIERARCHY!>Interface<!> by aImpl, <!CYCLIC_INHERITANCE_HIERARCHY!>I<Nested, Interface><!> {

        class Nested

        interface Interface
    }
}
