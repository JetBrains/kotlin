// !LANGUAGE: +InlineClasses
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

fun <T> foo(a: Result<T>): T = bar(a) {
    it.getOrThrow()
}

fun interface FunIFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: FunIFace<T, R>): R {
    return f.call(value)
}

fun box(): String {
    val res = foo<Int>(Result.success(40)) + 2
    return if (res != 42) "FAIL $res" else "OK"
}