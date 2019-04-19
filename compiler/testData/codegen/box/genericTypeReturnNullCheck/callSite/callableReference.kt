// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <T> bar() = run {
    fun foo() = null as T
    ::foo
}

fun box(): String {
    try {
        val x = bar<String>()
        x.invoke()
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should have been thrown"
}
