// ISSUE: KT-69408
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// REASON: For value classes, JVM backend needs usage of @kotlin.jvm.JvmInline annotation, which does not present in other backends
// The goal of this testcase is to test behavior of IR Inliner, which is not being used in JVM backends

value class MyResult<out T> constructor(
    val myValue: T
) {
    inline fun myGet(): T = myValue
}

fun box(): String {
    val foo: ()->MyResult<String> = { MyResult("OK") }
    return foo().myGet()
}
