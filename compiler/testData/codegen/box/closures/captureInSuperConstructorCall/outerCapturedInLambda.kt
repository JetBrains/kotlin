// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
open class Base(val callback: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base({ ok })
}

fun box(): String =
        Outer().Inner().callback()