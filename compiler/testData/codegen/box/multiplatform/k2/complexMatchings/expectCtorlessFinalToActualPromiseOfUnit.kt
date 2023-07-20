// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JS, JS_IR, JS_IR_ES6
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect class TestResult

expect fun createTestResult(): TestResult

fun commonBox(): String {
    val tr = createTestResult()
    val tr2 = if (true) tr else createTestResult()

    if (tr != tr2) return "c01"
    if (tr.hashCode() != tr2.hashCode()) return "c02"
    if (tr.toString() != tr2.toString()) return "c03"

    return "O"
}

// MODULE: js()()(common)
// FILE: js.kt

import kotlin.js.Promise

class PromiseOfUnit(executor: (resolve: (Unit) -> Unit, reject: (Throwable) -> Unit) -> Unit) : Promise<Unit>(executor)

actual typealias TestResult = PromiseOfUnit

actual fun createTestResult(): TestResult = PromiseOfUnit {
    resolve, reject ->
}

fun platformBox(): String {
    val tr = createTestResult()
    val tr2 = if (true) tr else createTestResult()

    if (tr != tr2) return "p01"
    if (tr.hashCode() != tr2.hashCode()) return "p02"
    if (tr.toString() != tr2.toString()) return "p03"

    return "K"
}

fun box() = commonBox() + platformBox()
