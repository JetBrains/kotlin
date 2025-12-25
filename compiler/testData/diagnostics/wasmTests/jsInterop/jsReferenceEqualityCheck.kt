// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// FIR_IDENTICAL
// TARGET_BACKEND: WASM
// LANGUAGE: +TurnTypeCheckWarningsIntoErrors

// FILE: jsReferenceEqualityCheck.kt

open class Base
class C(val x: Int) : Base()


val b = Base()
val c = C(1)
val str = ""

fun testEqualityOperations(obj: JsReference<C>) {
    val allowed = listOf(
        obj == c,
        obj != c,
        obj === c,
        obj !== c,
        obj == b,
        obj != b,
        obj === b,
        obj !== b,
        c == obj,
        b !== obj
    )
    val reported = listOf(
        <!EQUALITY_NOT_APPLICABLE!>obj == str<!>,
        <!EQUALITY_NOT_APPLICABLE!>obj != str<!>,
        <!EQUALITY_NOT_APPLICABLE!>obj === str<!>,
        <!EQUALITY_NOT_APPLICABLE!>obj !== str<!>,
        <!EQUALITY_NOT_APPLICABLE!>str == obj<!>,
        <!EQUALITY_NOT_APPLICABLE!>str !== obj<!>,
    )
}

fun testEqualityOperationsWithIntersection(obj: JsReference<C>) {
    if (obj !is C) return
    // here obj becomes "C & JsReference<C>" for checks

    val allowed = listOf(
        obj == c,
        obj != c,
        obj === c,
        obj !== c,
        obj == b,
        obj != b,
        obj === b,
        obj !== b,
        c == obj,
        b !== obj
    )
    val reported = listOf(
        <!EQUALITY_NOT_APPLICABLE!>obj == str<!>,
        <!EQUALITY_NOT_APPLICABLE!>obj != str<!>,
        <!EQUALITY_NOT_APPLICABLE!>obj === str<!>,
        <!EQUALITY_NOT_APPLICABLE!>obj !== str<!>,
        <!EQUALITY_NOT_APPLICABLE!>str == obj<!>,
        <!EQUALITY_NOT_APPLICABLE!>str !== obj<!>,
    )
}


fun testEqualityOperationsWithStarProjection(obj: JsReference<*>) {
    // all checks with star projection are allowed
    val allowed = listOf(
        obj == c,
        obj != c,
        obj === c,
        obj !== c,
        obj == b,
        obj != b,
        obj === b,
        obj !== b,
        c == obj,
        b !== obj,
        obj == str,
        obj != str,
        obj === str,
        obj !== str,
        str == obj,
        str !== obj
    )
}

fun testEqualityOperationsWithJsAnyn(obj: JsAny) {
    // all checks with JsAny are allowed
    val allowed = listOf(
        obj == c,
        obj != c,
        obj === c,
        obj !== c,
        obj == b,
        obj != b,
        obj === b,
        obj !== b,
        c == obj,
        b !== obj,
        obj == str,
        obj != str,
        obj === str,
        obj !== str,
        str == obj,
        str !== obj
    )
}
