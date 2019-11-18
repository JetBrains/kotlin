// IGNORE_BACKEND_FIR: JVM_IR
class A {
    companion object {
        fun ok() = "OK"
    }
}

fun box() = (A.Companion::ok)()