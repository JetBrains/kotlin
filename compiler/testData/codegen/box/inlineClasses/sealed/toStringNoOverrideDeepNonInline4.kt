// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    override fun toString(): String = "FAIL 1"
}

sealed inline class ICC: IC()

inline class ICString(val s: String): ICC() {
    override fun toString(): String = "O"
}

object ICO: ICC() {
    override fun toString(): String = "K"
}

fun toString(ic: IC): String = ic.toString()

fun box() = toString(ICString("O")) + toString(ICO)