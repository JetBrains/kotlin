// IGNORE_BACKEND_FIR: JVM_IR
class A {
    companion object {
        val result = "OK"
    }
}

typealias Alias = A.Companion

fun box(): String = Alias.result
