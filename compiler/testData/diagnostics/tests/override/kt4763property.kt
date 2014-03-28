trait P {
    var f: Number
}

open class Q {
    val x: Int = 42
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>R<!> : P, Q()

val s: Q = <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object<!> : Q(), P {}
