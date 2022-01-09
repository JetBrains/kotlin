// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    override fun toString(): String = "K"
}

inline class ICString(val s: String): IC() {
    override fun toString(): String = s
}

object ICO: IC()

fun toString(ic: IC): String = ic.toString()

fun box() = toString(ICString("O")) + toString(ICO)