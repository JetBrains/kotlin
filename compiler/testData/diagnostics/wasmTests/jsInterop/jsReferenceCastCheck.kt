// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// FIR_IDENTICAL
// TARGET_BACKEND: WASM
// MODULE: main

// FILE: jsReferenceCastCheck.kt

class C(val x: Int)

val c = C(1)

fun testTypeOperations(obj: JsReference<C>) {
    val allowed = listOf(
        obj as C,
        obj as? C,
        obj as Any,
        obj as? Any,
    )

    val reported = listOf(
        obj <!CAST_NEVER_SUCCEEDS!>as<!> String,
        obj <!CAST_NEVER_SUCCEEDS!>as?<!> String,
    )
}

fun testTypeOperationsWithIntersection(obj: JsReference<C>) {
    if (obj !is C) return
    // here obj becomes "C & JsReference<C>" for checks

    val allowed = listOf(
        obj as C,
        obj as? C,
        obj as Any,
        obj as? Any,
    )

    val reported = listOf(
        obj <!CAST_NEVER_SUCCEEDS!>as<!> String,
        obj <!CAST_NEVER_SUCCEEDS!>as?<!> String,
    )
}

fun testTypeOperations(obj: JsReference<*>) {
    val allowed = listOf(
        obj as String,
        obj as? String,
        obj as C,
        obj as? C,
        obj as Any,
        obj as? Any,
    )
}
