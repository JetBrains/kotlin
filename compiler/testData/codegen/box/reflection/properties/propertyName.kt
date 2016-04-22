class A {
    val a = ""
    val b = ""
}

fun box(): String {
    val a = A::a.name
    if (a != "a") return "Fail $a"
    val b = A::`b`.name
    if (b != "b") return "Fail $b"
    return "OK"
}
