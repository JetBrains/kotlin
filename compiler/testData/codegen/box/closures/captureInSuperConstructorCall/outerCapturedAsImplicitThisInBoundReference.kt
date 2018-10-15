// IGNORE_BACKEND: JVM_IR
abstract class Base(val fn: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base(::ok)
}

fun box() = Outer().Inner().fn()