// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.js.ExperimentalJsNoRuntime

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

package test

import kotlin.js.JsNoRuntime

expect interface RegularExpect

@JsNoRuntime
expect interface NoRuntimeExpect

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: js.kt
package test

// Expect is regular (no @JsNoRuntime) but actual is external interface: warn to add @JsNoRuntime on expect
actual external interface <!JS_ACTUAL_EXTERNAL_INTERFACE_WITHOUT_JS_NO_RUNTIME!>RegularExpect<!>

// Expect is annotated with @JsNoRuntime: no warning
actual external interface NoRuntimeExpect
