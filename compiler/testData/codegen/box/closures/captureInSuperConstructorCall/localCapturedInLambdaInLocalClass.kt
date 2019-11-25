// IGNORE_BACKEND_FIR: JVM_IR
open class Base(val fn: () -> String)

fun box(): String {
    val o = "O"

    class Local(k: String) : Base({ o + k })

    return Local("K").fn()
}