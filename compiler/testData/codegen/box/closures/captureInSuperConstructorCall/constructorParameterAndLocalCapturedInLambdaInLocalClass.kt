// IGNORE_BACKEND: JVM_IR
open class Base(val fn: () -> String)

fun box(): String {
    val o = "O"

    class Local {
        inner class Inner(k: String) : Base({ o + k })
    }

    return Local().Inner("K").fn()
}