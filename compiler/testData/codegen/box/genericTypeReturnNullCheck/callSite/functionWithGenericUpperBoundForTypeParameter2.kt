// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class Foo<K> {
    fun <T : K> foo(): T = null as T
}

fun box(): String {
    try {
        val x: Int = Foo<Number>().foo()
    } catch (e: NullPointerException) {
        return "OK"
    }
    return "Fail: NullPointerException should have been thrown"
}
