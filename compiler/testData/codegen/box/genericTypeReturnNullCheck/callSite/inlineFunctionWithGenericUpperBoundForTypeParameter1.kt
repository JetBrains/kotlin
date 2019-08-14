// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class Foo<K> {
    inline fun <T : K> foo(): T = null as T
}

fun box(): String {
    try {
        Foo<Number>().foo<Int>()
    } catch (e: NullPointerException) {
        return "OK"
    }
    return "Fail: NullPointerException should have been thrown"
}
