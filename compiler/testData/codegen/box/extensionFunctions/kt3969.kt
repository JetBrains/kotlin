var result = "Fail"

class A

fun A.inc(s: String = "OK"): A {
    result = s
    return this
}

fun box(): String {
    var a = A()
    a++
    if (result != "OK") return "Fail 1"

    result = "Fail"
    ++a

    return result
}