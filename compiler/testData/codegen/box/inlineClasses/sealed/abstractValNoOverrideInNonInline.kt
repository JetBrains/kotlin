// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    abstract val str: String
}

inline class ICString(val s: String): IC() {
    override val str: String; get() = s
}

object ICO: IC()

fun toString(ic: IC): String = ic.str

fun box() = toString(ICString("O")) + toString(ICO)