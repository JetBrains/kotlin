// IGNORE_BACKEND: JVM_IR
interface Callback {
    fun invoke(): String
}

open class Base(val fn: Callback)

fun box(): String {
    val ok = "OK"

    class Local : Base(
            object : Callback {
                override fun invoke() = ok
            })

    return Local().fn.invoke()
}