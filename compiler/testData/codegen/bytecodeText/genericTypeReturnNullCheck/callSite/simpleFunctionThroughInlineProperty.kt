// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM

fun <T> foo(): T = null as T

inline val x get() = foo<String>()

fun test() = x

// 2 IFNONNULL
// 2 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe
