var leftCalls = 0
var rightCalls = 0
var log = ""

data class Left(val a: String, val b: String)
data class Right(val b: String, val c: String)

fun left(): Left {
    leftCalls += 1
    log += "L"
    return Left("A", "B1")
}

fun right(): Right {
    rightCalls += 1
    log += "R"
    return Right("B2", "C")
}

fun foo(a: String, b: String, c: String): String {
    return "$a|$b|$c"
}

fun box(): String {
    val result = foo(...left(), ...right())
    return if (result == "A|B1|C" && leftCalls == 1 && rightCalls == 1 && log == "LR") {
        "OK"
    } else {
        "fail: $result|$leftCalls|$rightCalls|$log"
    }
}
