// ISSUE: KT-69408

// For value classes, JVM backend needs usage of @kotlin.jvm.JvmInline annotation, which is not present in other backends.
// The goal of this test case is to test behavior of IR inliner, which is not being used in JVM backend.
// DONT_TARGET_EXACT_BACKEND: JVM_IR

value class MyResult<out T> constructor(
    val myValue: T
) {
    inline fun myGet(): T = myValue
}

fun box(): String {
    val foo: ()->MyResult<String> = { MyResult("OK") }
    return foo().myGet()
}
