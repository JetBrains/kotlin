// NI_EXPECTED_FILE

class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

val test1 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>CStar<!>()
val test2 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>CIn<!>()
val test3 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>COut<!>()
val test4 = CT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
val test5 = CT<CT<*>>()
