class C(val x: String) {
    constructor(): this("")
}

typealias TC = C

val test1: C = TC("")
val test2: TC = TC("")
val test3: C = TC()
val test4: TC = TC()

val test5 = <!INAPPLICABLE_CANDIDATE!>TC<!>("", "")

interface Interface
typealias TI = Interface

object AnObject
typealias TO = AnObject

val test6 = <!UNRESOLVED_REFERENCE!>TI<!>()
val test6a = <!UNRESOLVED_REFERENCE!>Interface<!>()

val test7 = <!INAPPLICABLE_CANDIDATE!>TO<!>()
val test7a = <!INAPPLICABLE_CANDIDATE!>AnObject<!>()
