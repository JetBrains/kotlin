// IGNORE_BACKEND_FIR: JVM_IR
class Test {
    val a : String = "1"
    private val b : String get() = a

    fun outer() : Int {
        return b.length
    }
}

fun box() = if (Test().outer() == 1) "OK" else "fail"
