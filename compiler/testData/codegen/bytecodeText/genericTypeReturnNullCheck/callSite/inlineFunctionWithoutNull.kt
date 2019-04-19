// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM

inline fun <T> foo(): T = "" as T

fun test() = foo<String>()

// 0 IFNONNULL
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe