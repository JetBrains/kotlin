// IGNORE_BACKEND_FIR: JVM_IR
class B () {}

open class A(val b : B) {
    fun a(): A = object: A(b) {}
}

fun box() : String {
    val b = B()
    val a = A(b).a()

    if (a.b !== b) return "failed"

    return "OK"
}
