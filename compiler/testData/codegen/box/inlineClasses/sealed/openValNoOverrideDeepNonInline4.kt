// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    open val str: String; get() = "FAIL 1"
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