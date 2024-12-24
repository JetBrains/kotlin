// RUN_PIPELINE_TILL: BACKEND
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

expect interface Dummy {
    interface ExternalInterface
}

external class ExternalClass: Dummy.ExternalInterface

// MODULE: main-js()()(common)

actual external interface Dummy {
    actual interface ExternalInterface
}
