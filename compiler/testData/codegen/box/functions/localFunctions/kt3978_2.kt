// IGNORE_BACKEND_FIR: JVM_IR
class C() {
    fun box(): Int {
        fun local(i: Int = 1): Int {
            return i
        }
        return local()
    }
}

fun box(): String {
    return if (C().box() != 1) "fail" else "OK"
}