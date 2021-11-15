// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Location @JvmOverloads constructor(val value: String? = "OK")

fun box(): String = Location().value!!
