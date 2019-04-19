// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM

fun <T> bar(): () -> T {
    fun foo() = null as T
    return ::foo
}

fun main() {
    val x = bar<String>()
    x()
}

// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe