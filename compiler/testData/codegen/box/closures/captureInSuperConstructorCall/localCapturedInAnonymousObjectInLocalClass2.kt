// IGNORE_BACKEND_FIR: JVM_IR
interface Callback {
    fun invoke(): String
}

open class Base(val fn: Callback): Callback {
    override fun invoke() = fn.invoke()
}

fun box(): String {
    val ok = "OK"

    class Local : Base(
            object : Base(
                    object : Callback {
                        override fun invoke() = ok
                    }
            ) {}
    )

    return Local().fn.invoke()
}