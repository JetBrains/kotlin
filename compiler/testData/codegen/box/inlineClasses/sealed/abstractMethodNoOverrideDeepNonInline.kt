// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    abstract fun str(): String
}

inline class ICString(val s: String): IC() {
    override fun str(): String = s
}

sealed class ICC: IC() {
    override fun str(): String = "K"
}

object ICO: ICC()

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)