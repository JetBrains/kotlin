// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

val map = mapOf<String, Any?>("x" to null, "y" to null)
val x: String by map
val y: String? by map

fun box(): String {
    y
    return "OK"
}
