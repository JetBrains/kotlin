// ISSUE: KT-68975
// LANGUAGE: +IrInlinerBeforeKlibSerialization
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/nonLocalReturn.kt
inline fun foo(makeInt: () -> Int): Int {
    return js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"makeInt()"<!>)
}

fun box(): String {
    val i: Int = foo {
        return "OK"
    }
    error("unreachable: $i")
}
