// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

interface I {
    val str: String; get() = "FAIL 1"
}

sealed inline class IC: I {
    override val str: String; get() = "K"
}

inline class ICString(val s: String): IC() {
    override val str: String; get() = s
}

object ICO: IC()

fun toString(ic: IC): String = ic.str

fun box() = toString(ICString("O")) + toString(ICO)