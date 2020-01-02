// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

val test1 = <!UNRESOLVED_REFERENCE!>CStar<!>()
val test2 = <!UNRESOLVED_REFERENCE!>CIn<!>()
val test3 = <!UNRESOLVED_REFERENCE!>COut<!>()
val test4 = CT<*>()
val test5 = CT<CT<*>>()