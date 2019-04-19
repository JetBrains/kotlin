// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class Foo<T> {
    inner class Bar {
        fun foo(): T = null as T
    }
}

fun box(): String {
    try {
        val x = Foo<String>()
        val y = x.Bar()
        y.foo()
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should have been thrown"
}
