// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    override fun toString(): String = "FAIL 1"
}

inline class ICString(val s: String): IC() {
    override fun toString(): String = s
}

sealed class ICC: IC()

object ICO: ICC() {
    override fun toString(): String = "K"
}

val checkEquals(a: IC, b: IC) {
    if (a != b) error("$a == $b")
}

val checkNotEquals(a: IC, b: IC) {
    if (a == b) error("$a != $b")
}

fun box() {
    checkEquals(ICString("a"), ICString("a"))
    checkNotEquals(ICString("a"), ICString("b"))
    checkNotEquals(ICString("a"), ICO)
    checkEquals(ICO, ICO)
}