interface I<F, G>

val aImpl: A.Interface
    get() = null!!

object A : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!DELEGATION_NOT_TO_INTERFACE, UNRESOLVED_REFERENCE!>Interface<!> by aImpl, I<<!UNRESOLVED_REFERENCE!>Nested<!>, <!UNRESOLVED_REFERENCE!>Interface<!>> {

    class Nested

    interface Interface
}
