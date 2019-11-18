// IGNORE_BACKEND_FIR: JVM_IR
class Outer(val x: String) {
    abstract inner class InnerBase

    inner class Inner(val y: String) : OIB() {
        val z = x + y
    }
}

typealias OIB = Outer.InnerBase

fun box(): String =
        Outer("O").Inner("K").z
