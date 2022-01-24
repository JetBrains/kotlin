// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    fun str(i: Int = 0): String
}

sealed inline class IC: I

inline class ICString(val s: String): IC() {
    override fun str(i: Int): String = "O"
}

object ICO: IC() {
    override fun str(i: Int): String = "K"
}

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)