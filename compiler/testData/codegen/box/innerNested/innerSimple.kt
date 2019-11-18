// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    inner class Inner {
        fun box() = "OK"
    }
}

fun box() = Outer().Inner().box()
