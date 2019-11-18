// IGNORE_BACKEND_FIR: JVM_IR
class A {
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
