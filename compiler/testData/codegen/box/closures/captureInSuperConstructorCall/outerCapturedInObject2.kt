// IGNORE_BACKEND: JVM_IR
interface Callback {
    fun invoke(): String
}

open class Base(val callback: Callback)

class Outer {
    val ok = "OK"

    inner class Inner1 {
        inner class Inner2 : Base(
                object : Callback {
                    override fun invoke() = ok
                }
        )
    }
}

fun box(): String =
        Outer().Inner1().Inner2().callback.invoke()