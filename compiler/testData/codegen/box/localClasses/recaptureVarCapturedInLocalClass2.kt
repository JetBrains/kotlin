// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var x = ""

    class CapturesX {
        override fun toString() = x
    }

    fun outerFun1(): CapturesX {
        fun localFun() = CapturesX()
        return localFun()
    }

    x = "OK"
    return outerFun1().toString()
}