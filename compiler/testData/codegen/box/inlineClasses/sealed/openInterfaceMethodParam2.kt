// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    fun str(i: Int): String = "FAIL 1"
}

sealed inline class IC: I {
    override fun str(i: Int): String = "FAIL 2"
}

inline class ICString(val s: String): IC() {
    override fun str(i: Int): String = "O"
}

object ICO: IC() {
    override fun str(i: Int): String = "K"
}

fun toString(ic: IC): String = ic.str(0)

fun box() = toString(ICString("O")) + toString(ICO)