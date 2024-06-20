interface Inv<T>
interface Out<out T>
interface In<in T>

typealias InvStar = Inv<*>
typealias InvIn = Inv<in Int>
typealias InvOut = Inv<out Int>
typealias InvT<T> = Inv<T>

typealias OutStar = Out<*>
typealias OutOut = Out<<!REDUNDANT_PROJECTION!>out<!> Int>
typealias OutT<T> = Out<T>

typealias InStar = In<*>
typealias InIn = In<<!REDUNDANT_PROJECTION!>in<!> Int>
typealias InT<T> = In<T>

class Test1 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!>InvStar<!>
class Test2 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!>InvIn<!>
class Test3 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!>InvOut<!>
class Test4 : InvT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>>
class Test5 : InvT<InvT<*>>

class Test6 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!>OutStar<!>
class Test7 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!>OutOut<!>
class Test8 : OutT<Int>
class Test9 : OutT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE, REDUNDANT_PROJECTION!>out<!> Int>

class Test10 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!>InStar<!>
class Test11 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!>InIn<!>
class Test12 : InT<Int>
class Test13 : InT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE, REDUNDANT_PROJECTION!>in<!> Int>
