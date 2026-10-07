// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect interface <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>PlatformWork<!> : suspend (String) -> String

// MODULE: main-js()()(common)
// FILE: js.kt

actual interface <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>PlatformWork<!> : suspend (String) -> String

class <!IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE!>JsWork<!> : PlatformWork {
    override suspend fun invoke(value: String): String = value
}
