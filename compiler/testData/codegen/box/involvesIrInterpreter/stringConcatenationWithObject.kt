// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS

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
