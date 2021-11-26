// WITH_STDLIB
var result = "Fail"

object O {
    val z = 42
    init { result = "OK" }
}

class A {
    val x by O::z
}

fun box(): String {
    A()
    return result
}
