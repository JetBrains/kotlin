// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

inline fun <reified T> foo() = null as T

fun box(): String {
    try {
        foo<String>()
    } catch (e: TypeCastException) {
        return "OK"
    }
    return "Fail: TypeCastException should have been thrown"
}
