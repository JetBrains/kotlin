var firstCalls = 0
var secondCalls = 0
var thirdCalls = 0
var log = ""

data class First(val a: String, val b: String)
data class Second(val b: String, val c: String)
data class Third(val c: String, val d: String)

fun first(): First {
    firstCalls += 1
    log += "1"
    return First("A", "B1")
}

fun second(): Second {
    secondCalls += 1
    log += "2"
    return Second("B2", "C2")
}

fun third(): Third {
    thirdCalls += 1
    log += "3"
    return Third("C3", "D")
}

fun foo(a: String, b: String, c: String, d: String): String {
    return "$a|$b|$c|$d"
}

fun box(): String {
    val result = foo(...first(), ...second(), ...third())
    return if (result == "A|B1|C2|D" && firstCalls == 1 && secondCalls == 1 && thirdCalls == 1 && log == "123") {
        "OK"
    } else {
        "fail: $result|$firstCalls|$secondCalls|$thirdCalls|$log"
    }
}
