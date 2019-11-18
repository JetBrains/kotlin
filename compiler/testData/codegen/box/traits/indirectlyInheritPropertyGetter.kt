// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    val str: String
        get() = "OK"
}

interface B : A

class Impl : B

fun box() = Impl().str
