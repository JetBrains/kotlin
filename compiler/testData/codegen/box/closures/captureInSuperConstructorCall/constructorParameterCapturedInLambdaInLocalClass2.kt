// IGNORE_BACKEND: JVM_IR
open class Base(val fn: () -> String)

fun box(): String {
    class Local(val ok: String) {
        inner class Inner : Base({ ok })
    }

    return Local("OK").Inner().fn()
}