// RUN_PIPELINE_TILL: BACKEND
// IGNORE_PARTIAL_BODY_ANALYSIS
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
