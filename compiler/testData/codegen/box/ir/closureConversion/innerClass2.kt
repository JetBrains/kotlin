// IGNORE_BACKEND_FIR: JVM_IR
class Outer(val x: String) {
    inner class Inner(val y: String) {
        val z = x + y
    }
}

fun box() = Outer("O").Inner("K").z