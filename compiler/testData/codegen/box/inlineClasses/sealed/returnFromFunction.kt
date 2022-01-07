// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter, +SealedInlineClasses
// IGNORE_BACKEND: JVM

sealed inline class IC

inline class ICString(val s: String): IC()

object ICO: IC()

fun check(b: Boolean): IC = if (b) ICString("OK") else ICO

fun box(): String {
    if (check(true) !is ICString) return "FAIL 1"
    if (check(false) != ICO) return "FAIL 2"
    return "OK"
}