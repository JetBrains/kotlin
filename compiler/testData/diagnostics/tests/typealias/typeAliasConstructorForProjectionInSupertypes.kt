// !WITH_NEW_INFERENCE
open class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

class Test1 : <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>CStar()<!>
class Test2 : <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>CIn()<!>
class Test3 : <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>COut()<!>

class Test4 : CStar {
    constructor() : <!OI;EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED!>super<!>()
}

class Test5 : CT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>>()
