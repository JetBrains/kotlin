// IGNORE_BACKEND_FIR: JVM_IR
class T(val f : () -> Any?) {
    fun call() : Any? = f()
}
fun box(): String {
    return T({ "OK" }).call() as String
}
