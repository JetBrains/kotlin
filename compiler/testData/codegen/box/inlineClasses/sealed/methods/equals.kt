// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM
// WITH_STDLIB

sealed inline class IC

inline class ICString(val s: String): IC()

object ICO: IC()

fun checkEquals(a: IC, b: IC) {
    if (a != b) error("$a != $b")
}

fun checkNotEquals(a: IC, b: IC) {
    if (a == b) error("$a == $b")
}

fun box(): String {
    checkEquals(ICString("a"), ICString("a"))
    checkNotEquals(ICString("a"), ICString("b"))
    checkNotEquals(ICString("a"), ICO)
    checkEquals(ICO, ICO)
    return "OK"
}