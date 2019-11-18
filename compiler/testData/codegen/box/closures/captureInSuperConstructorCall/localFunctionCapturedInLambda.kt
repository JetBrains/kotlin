// IGNORE_BACKEND_FIR: JVM_IR
open class Base(val fn: () -> String)

fun box(): String {
    val x = "O"

    fun localFn() = x

    class Local(y: String) : Base({ localFn() + y })

    return Local("K").fn()
}