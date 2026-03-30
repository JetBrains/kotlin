var log = ""

data class Head(val a: String)
data class Tail(val b: String, val c: String)

fun head(): Head {
    log += "H"
    return Head("A")
}

fun tail(): Tail {
    log += "S"
    return Tail("B", "C")
}

fun overrideB(): String {
    log += "O"
    return "BB"
}

fun foo(a: String, b: String, c: String): String {
    return "$a|$b|$c"
}

fun box(): String {
    val result = foo(...head(), ...tail(), b = overrideB())
    return if (result == "A|BB|C" && log == "HSO") "OK" else "fail: $result|$log"
}
