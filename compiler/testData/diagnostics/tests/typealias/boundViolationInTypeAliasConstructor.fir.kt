class Num<T: Number>(val x: T)

typealias N<T> = Num<T>
typealias N2<T> = N<T>

val x1 = Num<<!UPPER_BOUND_VIOLATED!>String<!>>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
val x2 = N<<!UPPER_BOUND_VIOLATED!>String<!>>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
val x3 = N2<<!UPPER_BOUND_VIOLATED!>String<!>>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)

class TColl<T, C : Collection<T>>

typealias TC<T, C> = TColl<T, C>
typealias TC2<T, C> = TC<T, C>

val y1 = TColl<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>()
val y2 = TC<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>()
val y3 = TC2<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>()
