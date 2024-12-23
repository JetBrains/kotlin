// RUN_PIPELINE_TILL: BACKEND
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

expect interface <!NO_ACTUAL_FOR_EXPECT!>Dummy<!> {
    interface ExternalInterface
}

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>ExternalClass<!>: Dummy.ExternalInterface

// MODULE: main-js()()(common)

actual external interface Dummy {
    actual interface ExternalInterface
}
