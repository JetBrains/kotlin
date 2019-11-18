// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    val result = "OK"

    inner class Inner {
        fun foo() = result
    }
}

fun box(): String {
    val f = Outer.Inner::foo
    return f(Outer().Inner())
}
