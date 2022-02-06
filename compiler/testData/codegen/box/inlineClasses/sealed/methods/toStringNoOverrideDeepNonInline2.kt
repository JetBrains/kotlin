// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    override fun toString(): String = "FAIL 1"
}

sealed inline class ICC: IC() {
    override fun toString(): String = "O"
}

inline class ICString(val s: String): ICC()

object ICO: ICC() {
    override fun toString(): String = "K"
}

fun toString(ic: IC): String = ic.toString()

fun box(): String {
    val res = toString(ICString("O")) + toString(ICO)
    if (res != "ICString(s=O)K") return res
    return "OK"
}