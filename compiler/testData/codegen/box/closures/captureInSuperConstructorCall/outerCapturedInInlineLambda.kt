// IGNORE_BACKEND: JVM_IR
open class Base(val callback: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base(
            run {
                val x = ok
                { x }
            }
    )
}

fun box(): String =
        Outer().Inner().callback()