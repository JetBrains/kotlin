// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalContextParameters
// ISSUE: KT-86452

import kotlin.jvm.internal.CallableReference
import kotlin.test.assertEquals

context(a: String, b: Int)
fun foo(): String = a + b

class C(val x: String) {
    context(a: String)
    fun bar(): String = x + a
}

context(a: String)
val prop: String get() = a

context(a: String)
var mutableProp: String
    get() = a
    set(value) {}

fun plain(): String = "plain"

private fun Any.check(vararg values: Any?) {
    val args = (this as CallableReference).boundContextArguments ?: throw AssertionError("Fail: boundContextArguments is null")
    assertEquals(values.toList(), args.toList())
}

fun box(): String {
    val r: () -> String = context("A", 1) { ::foo }
    r.check("A", 1)

    val c = C("X")
    val rb: () -> String = context("A") { c::bar }
    rb.check("A")

    val rp = context("A") { ::prop }
    rp.check("A")

    val rm = context("A") { ::mutableProp }
    rm.check("A")

    assertEquals(null, (::plain as CallableReference).boundContextArguments)

    return "OK"
}
