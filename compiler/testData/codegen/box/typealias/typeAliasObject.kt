// IGNORE_BACKEND_FIR: JVM_IR
object OHolder {
    val O = "O"
}

typealias OHolderAlias = OHolder

class KHolder {
    companion object {
        val K = "K"
    }
}

typealias KHolderAlias = KHolder

fun box(): String = OHolderAlias.O + KHolderAlias.K
