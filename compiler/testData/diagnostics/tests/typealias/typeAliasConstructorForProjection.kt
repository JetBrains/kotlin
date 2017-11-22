// !WITH_NEW_INFERENCE
class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

val test1 = <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>CStar()<!>
val test2 = <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>CIn()<!>
val test3 = <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>COut()<!>
val test4 = CT<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>*<!>>()
val test5 = CT<CT<*>>()