// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    open fun str(i: Int = 0): String = "FAIL 1"
}

inline class ICString(val s: String): IC() {
    override fun str(i: Int): String = "O"
}

object ICO: IC() {
    override fun str(i: Int): String = "K"
}

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)