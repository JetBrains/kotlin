// IGNORE_BACKEND_FIR: JVM_IR
class X {
    fun g(x: () -> Boolean = { super.equals(this) }) = x()
}

fun box(): String {
    return if (X().g()) "OK" else "Fail: false"
}
