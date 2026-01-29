// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// FIR_IDENTICAL
// TARGET_BACKEND: WASM
// MODULE: main

// FILE: jsReferenceIsCheck.kt

class C(val x: Int)

val c = C(1)

fun testTypeOperations(obj: JsReference<C>) {
    val allowed = listOf(
        { obj is C },
        { obj !is C }
    )

    val reported = listOf(
        { <!USELESS_IS_CHECK!>obj is Any<!> },
        { <!USELESS_IS_CHECK!>obj !is Any<!> },
        { <!IMPOSSIBLE_IS_CHECK_WARNING!>obj is String<!> },
        { <!IMPOSSIBLE_IS_CHECK_WARNING!>obj !is String<!> },
    )
}

fun testTypeOperationsWithIntersection(obj: JsReference<C>) {
    if (obj !is C) return
    // here obj becomes "C & JsReference<C>" for checks

    val reported = listOf(
        { <!USELESS_IS_CHECK!>obj is C<!> },
        { <!USELESS_IS_CHECK!>obj !is C<!> },
        { <!USELESS_IS_CHECK!>obj is Any<!> },
        { <!USELESS_IS_CHECK!>obj !is Any<!> },
        { <!IMPOSSIBLE_IS_CHECK_WARNING!>obj is String<!> },
        { <!IMPOSSIBLE_IS_CHECK_WARNING!>obj !is String<!> },
    )
}

fun testTypeOperations(obj: JsReference<*>) {
    val allowed = listOf(
        { obj is C },
        { obj !is C },
    )

    val reported = listOf(
        { <!USELESS_IS_CHECK!>obj is Any<!> },
        { <!USELESS_IS_CHECK!>obj !is Any<!> },
    )
}
