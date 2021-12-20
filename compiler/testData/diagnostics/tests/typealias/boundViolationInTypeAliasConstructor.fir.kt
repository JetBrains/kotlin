class Num<T: Number>(val x: T)

typealias N<T> = Num<T>
typealias N2<T> = N<T>

val x1 = Num<<!UPPER_BOUND_VIOLATED!>String<!>>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
val x2 = <!UPPER_BOUND_VIOLATED!>N<String>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)<!>
val x3 = <!UPPER_BOUND_VIOLATED!>N2<String>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)<!>

class TColl<T, C : Collection<T>>

typealias TC<T, C> = TColl<T, C>
typealias TC2<T, C> = TC<T, C>

val y1 = TColl<Any, <!UPPER_BOUND_VIOLATED!>Any<!>>()
val y2 = <!UPPER_BOUND_VIOLATED!>TC<Any, Any>()<!>
val y3 = <!UPPER_BOUND_VIOLATED!>TC2<Any, Any>()<!>
