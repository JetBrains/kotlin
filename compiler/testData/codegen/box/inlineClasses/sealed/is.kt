// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICAny(val s: Any): IC()

value object ICO: IC()

fun check(ic: IC): String = when(ic) {
    is ICAny -> ic.s as String
    ICO -> "K"
}

fun box() = check(ICAny("O")) + check(ICO)