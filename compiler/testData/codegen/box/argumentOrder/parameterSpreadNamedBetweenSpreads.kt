var log = ""

data class Head(val a: String)
data class Tail(val b: String, val c: String)

fun head(): Head {
    log += "H"
    return Head("A")
}

fun overrideB(): String {
    log += "O"
    return "BB"
}

fun tail(): Tail {
    log += "T"
    return Tail("B", "C")
}

fun foo(a: String, b: String, c: String): String {
    return "$a|$b|$c"
}

fun box(): String {
    val result = foo(...head(), b = overrideB(), ...tail())
    return if (result == "A|BB|C" && log == "HOT") "OK" else "fail: $result|$log"
}
