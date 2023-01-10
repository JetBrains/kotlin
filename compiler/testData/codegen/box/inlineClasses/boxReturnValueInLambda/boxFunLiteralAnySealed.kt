// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: Any): IC()

fun useX(x: IC): String = (x as X).x as String

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call(fun(): IC { return X("OK") }))