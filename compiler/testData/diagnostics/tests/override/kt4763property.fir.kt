interface P {
    var f: Number
}

open class Q {
    val x: Int = 42
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class R<!> : P, Q()

val s: Q = object : Q(), P {}
