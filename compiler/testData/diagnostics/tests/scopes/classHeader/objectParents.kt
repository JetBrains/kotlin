interface I<F, G>

val aImpl: A.Interface
    get() = null!!

object A : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE, DELEGATION_NOT_TO_INTERFACE!>Interface<!> by aImpl, I<<!UNRESOLVED_REFERENCE!>Nested<!>, <!UNRESOLVED_REFERENCE!>Interface<!>> {

    class Nested

    interface Interface
}
