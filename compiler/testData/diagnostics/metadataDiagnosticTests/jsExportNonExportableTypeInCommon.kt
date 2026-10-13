// METADATA_TARGET_PLATFORMS: JVM, JS
// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.js.ExperimentalJsExport
// WITH_STDLIB
// ISSUE: KT-85629
import kotlin.js.JsExport

@JsExport
interface A {
    val b: B
}

interface B
