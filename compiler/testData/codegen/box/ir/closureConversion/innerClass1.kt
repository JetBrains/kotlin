// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    val x = "O"
    inner class Inner {
        val y = x + "K"
    }
}

fun box() = Outer().Inner().y