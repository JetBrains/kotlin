// IGNORE_BACKEND_FIR: JVM_IR
class A {
    companion object {
        val b = 0
        val c = b
        
        init {
            val d = b
        }
    }
}

fun box(): String {
    A()
    return if (A.c == A.b) "OK" else "Fail"
}
