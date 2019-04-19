// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

inline fun <reified T> foo(): T = null as T
inline fun <reified T> bar(): T = foo<T>()

fun box(): String {
    try {
        bar<String>()
    } catch (e: TypeCastException) {
        return "OK"
    }
    return "Fail: TypeCastException should have been thrown"
}
