// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICString(val s: String): IC()

value object ICO: IC()

fun check(ic: IC): String = when(ic) {
    is ICString -> ic.s
    ICO -> "FAIL"
}

fun box() = check(ICString("OK"))