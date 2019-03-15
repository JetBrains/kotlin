// IGNORE_BACKEND: JVM_IR
interface Callback {
    fun invoke(): String
}

open class Base(val callback: Callback)

class Outer {
    val ok = "OK"

    inner class Inner : Base(
            object : Callback {
                override fun invoke() =
                        (object : Callback {
                            override fun invoke() = ok
                        }).invoke()
            }
    )
}

fun box(): String =
        Outer().Inner().callback.invoke()