// RUN_PIPELINE_TILL: BACKEND
// IGNORE_PARTIAL_BODY_ANALYSIS
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

expect interface Dummy {
    interface ExternalInterface
}

external class ExternalClass: Dummy.ExternalInterface

// MODULE: main-js()()(common)

actual external interface <!JS_ACTUAL_EXTERNAL_INTERFACE_WHILE_EXPECT_WITHOUT_JS_NO_RUNTIME!>Dummy<!> {
    actual interface ExternalInterface
}
