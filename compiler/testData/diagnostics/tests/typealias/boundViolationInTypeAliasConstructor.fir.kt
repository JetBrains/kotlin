// !WITH_NEW_INFERENCE
class Num<T: Number>(val x: T)

typealias N<T> = Num<T>
typealias N2<T> = N<T>

val x1 = <!INAPPLICABLE_CANDIDATE!>Num<!><String>("")
val x2 = <!UPPER_BOUND_VIOLATED!>N<String>("")<!>
val x3 = <!UPPER_BOUND_VIOLATED!>N2<String>("")<!>

class TColl<T, C : Collection<T>>

typealias TC<T, C> = TColl<T, C>
typealias TC2<T, C> = TC<T, C>

val y1 = TColl<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>()
val y2 = <!UPPER_BOUND_VIOLATED!>TC<Any, Any>()<!>
val y3 = <!UPPER_BOUND_VIOLATED!>TC2<Any, Any>()<!>
