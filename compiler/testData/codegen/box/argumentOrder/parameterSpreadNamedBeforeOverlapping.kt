var log = ""

data class Left(val a: String, val b: String)
data class Right(val b: String, val c: String)

fun overrideA(): String {
    log += "N"
    return "AA"
}

fun left(): Left {
    log += "L"
    return Left("A", "B1")
}

fun right(): Right {
    log += "R"
    return Right("B2", "C")
}

fun foo(a: String, b: String, c: String): String {
    return "$a|$b|$c"
}

fun box(): String {
    val result = foo(a = overrideA(), ...left(), ...right())
    return if (result == "AA|B1|C" && log == "NLR") "OK" else "fail: $result|$log"
}
