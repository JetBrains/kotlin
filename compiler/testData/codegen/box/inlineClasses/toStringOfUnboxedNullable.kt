// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_LIGHT_ANALYSIS
inline class IC(val x: String)

fun IC?.foo() = toString() // `IC?` unboxed into `String?`
fun IC?.bar() = "$this"

fun assertEquals(a: String, b: String) {
    if (a != b) throw AssertionError("$a != $b")
}

fun box(): String {
    assertEquals((null as IC?).foo(), "null")
    assertEquals((null as IC?).foo(), (null as IC?).toString())
    assertEquals((null as IC?).foo(), (null as IC?).bar())
    assertEquals(IC("x").foo(), "IC(x=x)")
    assertEquals(IC("x").foo(), IC("x").toString())
    assertEquals(IC("x").foo(), IC("x").bar())
    return "OK"
}
