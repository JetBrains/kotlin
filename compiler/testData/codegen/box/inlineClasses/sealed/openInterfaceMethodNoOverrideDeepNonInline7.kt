// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    fun str(): String = "FAIL 1"
}

sealed inline class IC: I {
    override fun str(): String = "FAIL 2"
}

inline class ICString(val s: String): IC() {
    override fun str(): String = s
}

sealed class ICC: IC()

object ICO: ICC() {
    override fun str(): String = "K"
}

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)