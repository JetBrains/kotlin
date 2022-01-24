// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    fun str(): String
}

sealed inline class IC: I

sealed inline class ICC: IC() {
    override fun str(): String = "O"
}

inline class ICString(val s: String): ICC()

object ICO: ICC() {
    override fun str(): String = "K"
}

fun toString(ic: IC): String = ic.str()

fun box() = toString(ICString("O")) + toString(ICO)