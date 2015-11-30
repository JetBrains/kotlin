var result = "Fail"

class A

operator fun A.plusAssign(a: A, s: String = "OK") {
    result = s
}

fun box(): String {
    A() += A()
    return result
}
