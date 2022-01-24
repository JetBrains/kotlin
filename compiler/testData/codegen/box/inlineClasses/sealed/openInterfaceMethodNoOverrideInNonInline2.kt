// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    fun str(): String = "FAIL 1"
}

sealed inline class IC: I {
    override fun str(): String = "K"
}

inline class ICString(val s: String): IC() {
    override fun str(): String = s
}

object ICO: IC()

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)