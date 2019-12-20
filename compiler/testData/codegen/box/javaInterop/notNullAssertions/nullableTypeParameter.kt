// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
interface I {
    fun <T : String> f(x: T?) = x ?: "OK"
}

class C : I

fun box() = C().f<String>(null)
