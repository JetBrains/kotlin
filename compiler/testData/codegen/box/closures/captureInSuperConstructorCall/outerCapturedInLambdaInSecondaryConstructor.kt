// IGNORE_BACKEND: JVM_IR
open class Base(val callback: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base {
        constructor() : super({ ok })
    }
}

fun box(): String =
        Outer().Inner().callback()