// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

val map = mapOf<String, Any?>("x" to null, "y" to null)
val x: String by map
val y: String? by map

fun box(): String {
    y
    return "OK"
}
