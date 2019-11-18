// IGNORE_BACKEND_FIR: JVM_IR
class P {
    var x : Int = 0
        private set

    fun foo() {
        ({ x = 4 })()
    }
}

fun box() : String {
    val p = P()
    p.foo()
    return if (p.x == 4) "OK" else "fail"
}
