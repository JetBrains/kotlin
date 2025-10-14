// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/lambdaWithoutNonLocalControlflow.kt
// TARGET_BACKEND: JS_IR
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// WITH_STDLIB
import kotlin.test.*

inline fun testLambdaInline(
    block: (Unit) -> String,
): String {
    return js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"block()"<!>)
}

<!NOTHING_TO_INLINE!>inline<!> fun testLambdaNoInline(
    noinline block: (Unit) -> String,
): String {
    return js("block()")
}

inline fun testLambdaCrossInline(
    crossinline block: (Unit) -> String,
): String {
    return js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"block()"<!>)
}

fun box(): String {
    assertEquals("OK", testLambdaInline { "OK" })
    assertEquals("OK", testLambdaNoInline { "OK" })
    assertEquals("OK", testLambdaCrossInline { "OK" })
    return "OK"
}
