// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    abstract fun str(i: Int): String
}

inline class ICString(val s: String): IC() {
    override fun str(i: Int): String = "O"
}

object ICO: IC() {
    override fun str(i: Int): String = "K"
}

fun toString(ic: IC): String = ic.str(0)

fun box() = toString(ICString("O")) + toString(ICO)