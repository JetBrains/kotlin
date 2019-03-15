// !WITH_NEW_INFERENCE
class Num<T: Number>(val x: T)

typealias N<T> = Num<T>
typealias N2<T> = N<T>

val x1 = Num<<!UPPER_BOUND_VIOLATED!>String<!>>("")
val x2 = N<<!UPPER_BOUND_VIOLATED!>String<!>>("")
val x3 = N2<<!UPPER_BOUND_VIOLATED!>String<!>>("")

class TColl<T, C : Collection<T>>

typealias TC<T, C> = TColl<T, C>
typealias TC2<T, C> = TC<T, C>

val y1 = TColl<Any, <!NI;UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>()
val y2 = TC<Any, <!NI;UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>()
val y3 = TC2<Any, <!NI;UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>()
