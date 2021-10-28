// !LANGUAGE: +DefinitelyNonNullableTypes
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND_FIR: JVM_IR

fun <T> test(t: T) = t as (T & Any)

fun box(): String {
    try {
        test<Any?>(null)
    } catch (ex: NullPointerException) {
        return "FAIL: expected NPE"
    }
    return test("OK")
}