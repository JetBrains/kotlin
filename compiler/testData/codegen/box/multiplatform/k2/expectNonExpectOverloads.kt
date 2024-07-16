// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect fun bar(s: String): String

fun test_common(): Boolean {
    return bar("") == "actual"
}

// MODULE: intermediate1()()(common)
// FILE: intermediate1.kt

fun bar(s: String, x: Int = 0): String = "intermediate1"

fun test_intermediate1(): Boolean {
    // In the second case expect function wins because it doesn't have default value despite it has `expect` keyword (more specific shape)
    return bar("", 0) == "intermediate1" && bar("") == "actual"
}

// MODULE: intermediate2()()(intermediate1)
// FILE: intermediate2.kt

actual fun bar(s: String): String = "actual"

fun test_intermediate2(): Boolean {
    return bar("") == "actual"
}

// MODULE: platform()()(intermediate2)
// FILE: platform.kt

fun test_platform(): Boolean {
    return bar("") == "actual"
}

fun box(): String {
    if (!test_common()) return "FAIL test_common"
    if (!test_intermediate1()) return "FAIL test_intermediate1"
    if (!test_intermediate2()) return "FAIL test_intermediate2"
    if (!test_platform()) return "FAIL test_platform"
    return "OK"
}