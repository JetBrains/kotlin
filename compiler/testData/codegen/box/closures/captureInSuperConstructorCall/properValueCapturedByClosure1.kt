// IGNORE_BACKEND_FIR: JVM_IR
open class Outer(val fn: (() -> String)?) {
    companion object {
        val ok = "Fail: Companion.ok"
    }

    val ok = "Fail: Outer.ok"

    fun test(): Outer {
        val ok = "OK"
        class Local : Outer({ ok })

        return Local()
    }
}

fun box() = Outer(null).test().fn?.invoke()!!