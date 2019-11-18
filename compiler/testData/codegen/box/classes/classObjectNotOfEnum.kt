// IGNORE_BACKEND_FIR: JVM_IR
class A {
    companion object {
        fun values() = "O"
        fun valueOf() = "K"
    }
}

fun box() = A.values() + A.valueOf()
