// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    open fun str(): String = "FAIL 1"
}

sealed inline class ICC: IC()

inline class ICString(val s: String): ICC() {
    override fun str(): String = "O"
}

object ICO: ICC() {
    override fun str(): String = "K"
}

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)