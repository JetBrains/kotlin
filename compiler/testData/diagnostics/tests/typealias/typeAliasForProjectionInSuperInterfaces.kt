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

class Test1 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>InvStar<!>
class Test2 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>InvIn<!>
class Test3 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>InvOut<!>
class Test4 : InvT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>>
class Test5 : InvT<InvT<*>>

class Test6 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>OutStar<!>
class Test7 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>OutOut<!>
class Test8 : OutT<Int>
class Test9 : OutT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>out<!> Int>

class Test10 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>InStar<!>
class Test11 : <!EXPANDED_TYPE_CANNOT_BE_INHERITED!>InIn<!>
class Test12 : InT<Int>
class Test13 : InT<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>in<!> Int>