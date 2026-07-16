// RUN_PIPELINE_TILL: BACKEND
// See counterpart test without IR inliner in js/js.translator/testData/box/jsCode/referenceToKotlin.kt

<!NOTHING_TO_INLINE!>inline<!> fun inlineFun(arg: String): String {
    return js("p(arg)")
}

fun test12(): String {
    // test that the js() call in inlined inlineFun doesn't call this local p
    val p = { "wrong12" }
    return inlineFun("test12")
}

fun test13(): String {
    val p: (String, Int) -> String = { s, i -> s + i }

    // local inline functions are not supported, so we work this around by using an anonymous object with inline method
    val o = object {
        inline fun localInlineFun(arg: String, makeInt: () -> Int): String {
            return js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_ERROR!>"p(arg, makeInt())"<!>)
        }
    }
    val thirteen = 13
    return o.localInlineFun("test") { thirteen }
}
