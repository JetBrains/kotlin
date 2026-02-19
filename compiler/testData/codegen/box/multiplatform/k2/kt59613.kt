// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-59613

// MODULE: common
// FILE: common.kt
expect fun printJson(pretty: Boolean = false)
expect fun printJson(replacer: () -> Any?)

// MODULE: platform()()(common)
// FILE: platform.kt
actual fun printJson(replacer: () -> Any?) {}
actual fun printJson(pretty: Boolean) {}

fun box(): String {
    printJson()
    return "OK"
}
