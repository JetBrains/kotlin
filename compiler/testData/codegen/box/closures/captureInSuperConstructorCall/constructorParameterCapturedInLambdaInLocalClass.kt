// IGNORE_BACKEND_FIR: JVM_IR
open class Base(val fn: () -> String)

fun box(): String {
    class Local {
        inner class Inner(ok: String) : Base({ ok })
    }

    return Local().Inner("OK").fn()
}