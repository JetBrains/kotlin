class In<in T>
class Out<out T>
class Inv<T>

typealias In1<T> = In<T>
typealias In2<T> = In<<!REDUNDANT_PROJECTION!>in<!> T>
typealias In3<T> = In<<!CONFLICTING_PROJECTION!>out<!> T>
typealias In4<T> = In<*>

typealias Out1<T> = Out<T>
typealias Out2<T> = Out<<!CONFLICTING_PROJECTION!>in<!> T>
typealias Out3<T> = Out<<!REDUNDANT_PROJECTION!>out<!> T>
typealias Out4<T> = Out<*>

typealias Inv1<T> = Inv<T>
typealias Inv2<T> = Inv<in T>
typealias Inv3<T> = Inv<out T>
typealias Inv4<T> = Inv<*>

val inv1: Inv1<Int> = Inv<Int>()