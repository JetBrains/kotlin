// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE

object K : Code("K")

open class Code(val x: String) {
    override fun toString() = "$x"
}

class O {
    companion object: Code("O")
}

fun box(): String {
    return "$O" + "$K" // must not be evaluated during compile time
}