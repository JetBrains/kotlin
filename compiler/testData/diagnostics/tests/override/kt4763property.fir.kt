interface P {
    var f: Number
}

open class Q {
    val x: Int = 42
}

class R : P, Q()

val s: Q = object : Q(), P {}
