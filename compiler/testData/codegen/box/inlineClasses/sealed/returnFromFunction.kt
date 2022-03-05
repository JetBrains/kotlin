// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICString(val s: String): IC()

value object ICO: IC()

fun check(b: Boolean): IC = if (b) ICString("OK") else ICO

fun box(): String {
    if (check(true) !is ICString) return "FAIL 1"
    if (check(false) != ICO) return "FAIL 2"
    return "OK"
}