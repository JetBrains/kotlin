var log = ""

data class Tail(val b: String, val c: String)

fun head(): String {
    log += "H"
    return "A"
}

fun tail(): Tail {
    log += "S"
    return Tail("B", "C")
}

fun foo(a: String, b: String, c: String): String {
    return a + b + c
}

fun box(): String {
    val result = foo(head(), ...tail())
    return if (result == "ABC" && log == "HS") "OK" else "fail: $result|$log"
}
