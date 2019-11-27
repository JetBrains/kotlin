// IGNORE_BACKEND_FIR: JVM_IR
open class Factory(p: Int)

class A {
    companion object : Factory(1)
}

fun box() : String {
    A
    return "OK"
}