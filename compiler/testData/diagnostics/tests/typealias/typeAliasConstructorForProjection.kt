// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

val test1 = <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED{OI}!>CStar()<!>
val test2 = <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED{OI}!>CIn()<!>
val test3 = <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED{OI}!>COut()<!>
val test4 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}!>CT<!><<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
val test5 = CT<CT<*>>()
