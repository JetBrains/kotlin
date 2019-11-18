// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE

class A : ArrayList<String>() {
    override val size: Int get() = super.size + 56
}

fun box(): String {
    val a = A()
    if (a.size != 56) return "fail: ${a.size}"

    return "OK"
}
