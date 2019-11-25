// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM

class A(val x: String) {
    open inner class AB(val y: String) {
        fun bar() = x + y
    }
}

fun A.foo(u: String, v: String, w: String): A.AB {
    class FooC(z: String) : A.AB("$z$v$w")
    return FooC(u)
}

fun box(): String {
    val r = A("1").foo("2", "3", "4").bar()
    if (r != "1234") return "fail: $r"

    return "OK"
}