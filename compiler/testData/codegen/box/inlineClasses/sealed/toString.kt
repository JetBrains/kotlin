// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC

inline class ICString(val s: String): IC() {
    override fun toString(): String = s
}

object ICO: IC() {
    override fun toString(): String = "K"
}

fun toString(ic: IC): String = ic.toString()

fun box() = toString(ICString("O")) + toString(ICO)