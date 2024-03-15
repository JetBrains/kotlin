// MODULE: commonjs
// TARGET_PLATFORM: JS
// FILE: commonjs.kt

expect interface ExternalInterface

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE{METADATA}!>ExternalClass<!>: ExternalInterface

// MODULE: js()()(commonjs)
// TARGET_PLATFORM: JS
// FILE: js.kt

actual external interface ExternalInterface
