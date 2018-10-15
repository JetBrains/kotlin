// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Result<T>(val a: Any?)

fun box(): String {
    val a = Result<Int>(1) // valueOf
    val b = Result<String>("sample")
    val c = Result<Result<Int>>(a)
    val d = Result<Result<Int>>(Result<Int>(1)) // valueOf

    if (a.a !is Int) throw AssertionError()

    if (b.a !is String) throw AssertionError()

    if (c.a !is Result<*>) throw AssertionError()
    val ca = c.a as Result<*>
    if (ca.a !is Int) throw AssertionError()

    if (d.a !is Result<*>) throw AssertionError()
    val da = d.a as Result<*>
    if (da.a !is Int) throw AssertionError()

    return "OK"
}
