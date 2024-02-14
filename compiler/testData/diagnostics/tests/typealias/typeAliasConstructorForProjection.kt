// ISSUE: KT-60305
// NI_EXPECTED_FILE

class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

val test1 = CStar()
val test2 = CIn()
val test3 = COut()
val test4 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>CT<!><<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
val test5 = CT<CT<*>>()
