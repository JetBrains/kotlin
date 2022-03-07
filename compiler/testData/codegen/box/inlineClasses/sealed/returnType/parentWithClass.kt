// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

value class C(val ok: String): IC()

fun foo(): IC = C("OK")

fun box(): String = (foo() as C).ok