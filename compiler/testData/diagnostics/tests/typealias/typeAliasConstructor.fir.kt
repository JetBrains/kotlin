class C(val x: String) {
    constructor(): this("")
}

typealias TC = C

val test1: C = TC("")
val test2: TC = TC("")
val test3: C = TC()
val test4: TC = TC()

val test5 = <!NONE_APPLICABLE!>TC<!>("", "")

interface Interface
typealias TI = Interface

object AnObject
typealias TO = AnObject

val test6 = <!INTERFACE_AS_FUNCTION!>TI<!>()
val test6a = <!INTERFACE_AS_FUNCTION!>Interface<!>()

val test7 = <!UNRESOLVED_REFERENCE!>TO<!>()
val test7a = <!UNRESOLVED_REFERENCE!>AnObject<!>()
