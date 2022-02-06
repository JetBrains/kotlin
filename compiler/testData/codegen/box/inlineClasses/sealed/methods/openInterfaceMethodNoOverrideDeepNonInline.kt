// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    fun str(): String = "FAIL 1"
}

sealed inline class IC: I

inline class ICString(val s: String): IC() {
    override fun str(): String = s
}

sealed class ICC: IC() {
    override fun str(): String = "K"
}

object ICO: ICC()

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)