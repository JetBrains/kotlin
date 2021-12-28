// WITH_STDLIB
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: String>(val x: T)

fun <T: String> IC<T>?.foo() = toString() // `IC?` unboxed into `String?`
fun <T: String> IC<T>?.bar() = "$this"

fun assertEquals(a: String, b: String) {
    if (a != b) throw AssertionError("$a != $b")
}

fun box(): String {
    assertEquals((null as IC<String>?).foo(), "null")
    assertEquals((null as IC<String>?).foo(), (null as IC<String>?).toString())
    assertEquals((null as IC<String>?).foo(), (null as IC<String>?).bar())
    assertEquals(IC("x").foo(), "IC(x=x)")
    assertEquals(IC("x").foo(), IC("x").toString())
    assertEquals(IC("x").foo(), IC("x").bar())
    return "OK"
}
