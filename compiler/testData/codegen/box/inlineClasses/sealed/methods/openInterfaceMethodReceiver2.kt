// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    fun Int.str(): String = "FAIL 1"
}

sealed inline class IC: I {
    override fun Int.str(): String = "FAIL 2"
}

inline class ICString(val s: String): IC() {
    override fun Int.str(): String = "O"
}

object ICO: IC() {
    override fun Int.str(): String = "K"
}

fun toString(ic: IC): String = with(ic) { 0.str() }

fun box() = toString(ICString("O")) + toString(ICO)