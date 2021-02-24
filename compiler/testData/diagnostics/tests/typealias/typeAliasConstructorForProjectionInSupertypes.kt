// !WITH_NEW_INFERENCE
open class C<T>

typealias CStar = C<*>
typealias CIn = C<in Int>
typealias COut = C<out Int>
typealias CT<T> = C<T>

class Test1 : <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED{OI}!>CStar()<!>
class Test2 : <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED{OI}!>CIn()<!>
class Test3 : <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED{OI}!>COut()<!>

class Test4 : CStar {
    constructor() : <!EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED{OI}!>super<!>()
}

class Test5 : CT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>>()
