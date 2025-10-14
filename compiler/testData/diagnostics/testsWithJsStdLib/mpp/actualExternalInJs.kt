// FIR_IDENTICAL
// RUN_PIPELINE_TILL: KLIB
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

expect interface Dummy {
    interface ExternalInterface
}

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE{METADATA}!>ExternalClass<!>: Dummy.ExternalInterface

// MODULE: main-js()()(common)

actual external interface Dummy {
    actual interface ExternalInterface
}
