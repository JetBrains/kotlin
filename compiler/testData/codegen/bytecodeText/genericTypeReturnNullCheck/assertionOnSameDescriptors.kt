// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR

fun <T> foo(): T = null as T

fun box(): String {
    val y: () -> () -> String = { ::foo }

    try {
        y()()
    } catch (e: KotlinNullPointerException) {
        try {
            y()()
        } catch (e: KotlinNullPointerException) {
            return "OK"
        }
    }
    return "Fail: KotlinNullPointerException should have been thrown"
}

// 5 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNull
