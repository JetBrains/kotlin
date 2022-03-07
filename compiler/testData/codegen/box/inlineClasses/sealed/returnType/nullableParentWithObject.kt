// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

value object O: IC() {
    val ok = "OK"
}

fun foo(): IC? = O

fun box(): String = (foo() as O).ok