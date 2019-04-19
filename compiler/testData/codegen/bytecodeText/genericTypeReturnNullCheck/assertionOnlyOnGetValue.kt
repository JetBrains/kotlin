// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR

fun map() = mapOf<String, Any?>("x" to null)
val x: String by map()

fun main() = x

// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNull
