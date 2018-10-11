// IGNORE_BACKEND: JVM_IR
interface Callback {
    fun invoke(): String
}

open class Base(val callback: Callback)

class Outer {
    val ok = "OK"

    inner class Inner : Base(
            object : Callback {
                override fun invoke() = ok
            }
    )
}

fun box(): String =
        Outer().Inner().callback.invoke()