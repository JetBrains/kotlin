// IGNORE_BACKEND_FIR: JVM_IR
var x = 0

class A {
    fun f() = if (x == 1) "OK" else "Fail $x"
}

fun callTwice(f: () -> String): String {
    f()
    return f()
}

fun box(): String {
    return callTwice(({ x++; A() }())::f)
}
