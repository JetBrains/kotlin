// !WITH_NEW_INFERENCE
class Num<T: Number>(val x: T)

typealias N<T> = Num<T>
typealias N2<T> = N<T>

val x1 = Num<String>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
val x2 = N<String>("")
val x3 = N2<String>("")

class TColl<T, C : Collection<T>>

typealias TC<T, C> = TColl<T, C>
typealias TC2<T, C> = TC<T, C>

val y1 = TColl<Any, <!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED!>Any<!>>()
val y2 = TC<Any, Any>()
val y3 = TC2<Any, Any>()
