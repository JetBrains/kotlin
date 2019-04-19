// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

class Foo<K> {
    inline fun <reified T : K> foo(): T = null as T
}

fun box(): String {
    try {
        val x: Int = Foo<Number>().foo()
    } catch (e: TypeCastException) {
        return "OK"
    }
    return "Fail: TypeCastException should have been thrown"
}
