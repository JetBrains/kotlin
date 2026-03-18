// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Location <T : String?> @JvmOverloads constructor(val value: T = "OK" as T)

fun box(): String = Location<String?>().value!!
