var result = "Fail"

class A

fun A.plus(a: A, s: String = "OK"): A {
    result = s
    return this
}

fun box(): String {
    var a = A()
    a += A()
    return result
}
