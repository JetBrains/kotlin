// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Location @JvmOverloads constructor(val value: String? = "OK")

fun box(): String = Location().value!!
