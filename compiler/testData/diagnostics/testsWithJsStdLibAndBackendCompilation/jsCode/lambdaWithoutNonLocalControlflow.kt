// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/lambdaWithoutNonLocalControlflow.kt
// TARGET_BACKEND: JS_IR
// WITH_STDLIB
import kotlin.test.*

inline fun testLambdaInline(
    <!UNUSED_PARAMETER!>block<!>: (Unit) -> String,
): String {
    return js("block()")
}

<!NOTHING_TO_INLINE!>inline<!> fun testLambdaNoInline(
    noinline <!UNUSED_PARAMETER!>block<!>: (Unit) -> String,
): String {
    return js("block()")
}

inline fun testLambdaCrossInline(
    crossinline <!UNUSED_PARAMETER!>block<!>: (Unit) -> String,
): String {
    return js("block()")
}

fun box(): String {
    assertEquals("OK", testLambdaInline { "OK" })
    assertEquals("OK", testLambdaNoInline { "OK" })
    assertEquals("OK", testLambdaCrossInline { "OK" })
    return "OK"
}
