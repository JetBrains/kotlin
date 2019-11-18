// IGNORE_BACKEND_FIR: JVM_IR
class A {
    companion object {}
    enum class E {
        OK
    }
}

fun box() = A.E.OK.toString()
