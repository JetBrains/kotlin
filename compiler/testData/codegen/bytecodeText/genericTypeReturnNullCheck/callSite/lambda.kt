// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM

fun <T> bar(): () -> T = { null as T }

fun main() {
    val x = bar<String>()
    val y = x()
}

// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe