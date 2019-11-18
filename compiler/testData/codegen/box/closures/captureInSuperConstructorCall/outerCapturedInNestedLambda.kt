// IGNORE_BACKEND_FIR: JVM_IR
open class Base(val callback: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base({ { ok }() })
}

fun box(): String =
        Outer().Inner().callback()
