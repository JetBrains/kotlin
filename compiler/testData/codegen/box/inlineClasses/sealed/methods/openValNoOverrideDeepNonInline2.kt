// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    open val str: String; get() = "FAIL 1"
}

sealed inline class ICC: IC() {
    override val str: String; get() = "O"
}

inline class ICString(val s: String): ICC()

object ICO: ICC() {
    override val str: String; get() = "K"
}

fun toString(ic: IC): String = ic.str

fun box() = toString(ICString("O")) + toString(ICO)