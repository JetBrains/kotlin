// IGNORE_BACKEND_FIR: JVM_IR
open class Outer(val fn: (() -> String)?) {
    companion object {
        val ok = "OK"
    }

    val ok = "Fail: Outer.ok"

    inner class Inner : Outer({ ok })
}

fun box() = Outer(null).Inner().fn?.invoke()!!