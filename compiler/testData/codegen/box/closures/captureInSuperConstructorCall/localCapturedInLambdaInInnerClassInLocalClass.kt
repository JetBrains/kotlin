// IGNORE_BACKEND: JVM_IR
open class Base(val fn: () -> String)

fun box(): String {
    val ok = "OK"

    class Local {
        inner class Inner : Base({ ok })
    }

    return Local().Inner().fn()
}