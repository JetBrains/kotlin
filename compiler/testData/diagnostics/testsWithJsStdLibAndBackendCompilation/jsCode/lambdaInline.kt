// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68975, KT-66181
// See counterpart test without IR inliner in js/js.translator/testData/box/jsCode/lambdaInline.kt
external fun p(s: String, n: () -> String): String

inline fun foo(arg: String, makeString: () -> String): String {
    return js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_ERROR!>"p(arg, makeString)"<!>)
}

fun box() = foo("O") { "K" }
