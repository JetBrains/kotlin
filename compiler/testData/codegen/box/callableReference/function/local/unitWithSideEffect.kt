// IGNORE_BACKEND_FIR: JVM_IR
var state = 23

fun box(): String {
    fun incrementState(inc: Int) {
        state += inc
    }

    val inc = ::incrementState
    inc(12)
    inc(-5)
    inc(27)
    inc(-15)

    return if (state == 42) "OK" else "Fail $state"
}
