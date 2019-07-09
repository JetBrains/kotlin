// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// FILE: test.kt
inline class A(val s: String) {
    fun fromResult(x: Result<String>) =
        x.getOrNull() ?: s
}

fun box(): String {
    return A("Fail").fromResult(Result.success("OK"))
}

// @TestKt.class:
// 1 INVOKESTATIC A.fromResult