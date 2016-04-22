class A {
    val a = ""
    val b = ""
}

val c = ""

fun box(): String {
    if ("a" != A::a.name) return "Fail a"
    if ("b" != A::`b`.name) return "Fail b"
    if ("c" != ::c.name) return "Fail c"
    return "OK"
}
