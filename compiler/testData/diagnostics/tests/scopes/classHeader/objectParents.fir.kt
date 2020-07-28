interface I<F, G>

val aImpl: A.Interface
    get() = null!!

object A : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE!>Interface<!> by aImpl, <!UNRESOLVED_REFERENCE!>I<Nested, Interface><!> {

    class Nested

    interface Interface
}
