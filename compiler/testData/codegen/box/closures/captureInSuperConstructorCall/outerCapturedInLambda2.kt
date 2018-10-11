// IGNORE_BACKEND: JVM_IR
open class Base(val callback: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner1 {
        inner class Inner2 : Base({ ok })
    }

}

fun box(): String =
        Outer().Inner1().Inner2().callback()