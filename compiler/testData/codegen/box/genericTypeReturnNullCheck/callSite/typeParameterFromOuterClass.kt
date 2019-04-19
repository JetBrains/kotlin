// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
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
    } catch (e: NullPointerException) {
        return "OK"
    }
    return "Fail: NullPointerException should have been thrown"
}
