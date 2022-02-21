// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Result<T: Any>(val a: T?)

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
