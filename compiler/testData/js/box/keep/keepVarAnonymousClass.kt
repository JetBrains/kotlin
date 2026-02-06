// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// KEEP: DenotedFace

// MODULE: keep_var
// FILE: lib.kt

interface DenotedFace {
    fun keepMeA() = 7
    fun keepMeB(): Int
}

@JsExport
val denotedFaceTrigger = object : DenotedFace { override fun keepMeB() = 8 }

// FILE: test.js
function box() {
    var a = this["keep_var"].denotedFaceTrigger

    if (a.keepMeA_ds0nq4_k$() != 7) return "fail 1"
    if (a.keepMeB_ds0nq3_k$() != 8) return "fail 2"

    return "OK"
}