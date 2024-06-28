// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: ANY
// FIR status: expect/actual in the same module
//   also used as a workaround for no multi-module support in K1 box tests

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

// FILE: actual.kt

actual typealias TestResult = Unit

actual fun createTestResult(): TestResult = TestResult

fun platformBox(): String {
    val tr = createTestResult()
    val tr2 = if (true) tr else createTestResult()

    if (tr != tr2) return "p01"
    if (tr.hashCode() != tr2.hashCode()) return "p02"
    if (tr.toString() != tr2.toString()) return "p03"

    return "K"
}

fun box() = commonBox() + platformBox()
