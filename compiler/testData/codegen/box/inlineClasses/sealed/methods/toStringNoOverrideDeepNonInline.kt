// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    override fun toString(): String = "FAIL 1"
}

inline class ICString(val s: String): IC() {
    override fun toString(): String = s
}

sealed class ICC: IC() {
    override fun toString(): String = "K"
}

object ICO: ICC()

fun toString(ic: IC): String = ic.toString()

fun box() = toString(ICString("O")) + toString(ICO)