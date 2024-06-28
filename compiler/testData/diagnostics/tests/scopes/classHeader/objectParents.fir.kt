interface I<F, G>

val aImpl: A.Interface
    get() = null!!

object A : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Interface<!> by aImpl, I<<!UNRESOLVED_REFERENCE!>Nested<!>, <!UNRESOLVED_REFERENCE!>Interface<!>> {

    class Nested

    interface Interface
}
