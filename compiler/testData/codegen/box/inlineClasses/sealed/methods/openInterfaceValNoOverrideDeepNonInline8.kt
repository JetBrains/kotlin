// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    val str: String; get() = "FAIL 1"
}

sealed inline class IC: I {
    override val str: String; get() = "FAIL 2"
}

sealed inline class ICC: IC()

inline class ICString(val s: String): ICC() {
    override val str: String; get() = "O"
}

object ICO: ICC() {
    override val str: String; get() = "K"
}

fun toString(ic: IC): String = ic.str

fun box() = toString(ICString("O")) + toString(ICO)