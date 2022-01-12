// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC {
    override fun toString(): String = "FAIL 1"
}

sealed inline class ICC: IC()

inline class ICString(val s: String): ICC() {
    override fun toString(): String = "O"
}

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