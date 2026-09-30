// ISSUE: KT-89770
// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -OPT_IN_USAGE
// WITH_STDLIB
// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE, WASM_WASI

// MODULE: web
// METADATA_TARGET_PLATFORMS: JS, WasmJs
// FILE: web.kt
external class Foo : JsAny

// MODULE: platform()()(web)
// FILE: main.kt
fun box(): String = "OK"
