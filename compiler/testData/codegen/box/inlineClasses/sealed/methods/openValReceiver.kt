// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    open val Int.str: String; get() = "FAIL 1"
}

inline class ICString(val s: String): IC() {
    override val Int.str: String; get() = s
}

object ICO: IC() {
    override val Int.str: String; get() = "K"
}

fun toString(ic: IC): String = with(ic) { 0.str }

fun box() = toString(ICString("O")) + toString(ICO)