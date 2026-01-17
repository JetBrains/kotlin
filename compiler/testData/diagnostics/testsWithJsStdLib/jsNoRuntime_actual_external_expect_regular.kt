// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.js.ExperimentalJsNoRuntime

// MODULE: common
// FILE: common.kt

package test

import kotlin.js.JsNoRuntime

inline fun <reified T> foo(x: T) {}

@JsNoRuntime
expect interface NoRuntimeExpect

@JsNoRuntime
expect interface AnotherNoRuntimeExpect

@JsNoRuntime
expect interface AndOneMore

expect interface RegularExpect : NoRuntimeExpect

fun commonIsCheck(a: Any) {
    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a is NoRuntimeExpect<!>) {}
    if (<!JS_NO_RUNTIME_FORBIDDEN_IS_CHECK!>a !is NoRuntimeExpect<!>) {}

    if (a is RegularExpect) {}
    if (a !is RegularExpect) {}
}

fun commonAsCast(a: Any) {
    val y = <!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>a as? NoRuntimeExpect<!>
    val x = <!JS_NO_RUNTIME_FORBIDDEN_AS_CAST!>a as NoRuntimeExpect<!>

    val d = a as? RegularExpect
    val e = a as RegularExpect
}

fun commonClassRef() {
    val k = <!JS_NO_RUNTIME_FORBIDDEN_CLASS_REFERENCE!>NoRuntimeExpect::class<!>
    val w = RegularExpect::class
}

fun commonReified(ci: NoRuntimeExpect) {
    <!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>foo<!>(ci)
    foo<<!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>NoRuntimeExpect<!>>(ci)
}

fun commonReifiedUsageOfWithRuntime(w: RegularExpect) {
    foo(w)
    foo<RegularExpect>(w)
    foo<<!JS_NO_RUNTIME_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>NoRuntimeExpect<!>>(w)
}

// MODULE: js()()(common)
// FILE: js.kt
package test

// Expect is regular (no @JsNoRuntime) but actual is external interface: warn to add @JsNoRuntime on expect
actual external interface <!JS_ACTUAL_EXTERNAL_INTERFACE_WHILE_EXPECT_WITHOUT_JS_NO_RUNTIME!>RegularExpect<!> : NoRuntimeExpect

// Expect is annotated with @JsNoRuntime: no warning
actual external interface NoRuntimeExpect

actual interface <!JS_NO_RUNTIME_ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>AnotherNoRuntimeExpect<!>

@JsNoRuntime
actual interface AndOneMore

fun jsPlatformIsCheck(a: Any) {
    if (<!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>a is NoRuntimeExpect<!>) {}
    if (<!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>a !is NoRuntimeExpect<!>) {}

    if (<!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>a is RegularExpect<!>) {}
    if (<!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>a !is RegularExpect<!>) {}
}

fun jsPlatformAsCast(a: Any) {
    val y = <!UNCHECKED_CAST_TO_EXTERNAL_INTERFACE!>a as? NoRuntimeExpect<!>
    val x = <!UNCHECKED_CAST_TO_EXTERNAL_INTERFACE!>a as NoRuntimeExpect<!>

    val d = <!UNCHECKED_CAST_TO_EXTERNAL_INTERFACE!>a as? RegularExpect<!>
    val e = <!UNCHECKED_CAST_TO_EXTERNAL_INTERFACE!>a as RegularExpect<!>
}

fun jsPlatformClassRef() {
    val k = <!EXTERNAL_INTERFACE_AS_CLASS_LITERAL!>NoRuntimeExpect::class<!>
    val w = <!EXTERNAL_INTERFACE_AS_CLASS_LITERAL!>RegularExpect::class<!>
}

fun jsPlatformReified(ci: NoRuntimeExpect) {
    <!EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>foo<!>(ci)
    foo<<!EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>NoRuntimeExpect<!>>(ci)
}

fun jsPlatformReifiedUsageOfWithRuntime(w: RegularExpect) {
    <!EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>foo<!>(w)
    foo<<!EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>RegularExpect<!>>(w)
    foo<<!EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT!>NoRuntimeExpect<!>>(w)
}
