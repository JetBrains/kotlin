// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58896
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun f(param: Int): String

fun f(param: Any) = "$param: Any"

expect val p: String

fun commonFun() = "${f(1)}; ${f("s")}"

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun f(param: Int) = "$param: Int"

actual val p = "OK"

fun platformFun() = "${f(1)}; ${f("s")}"

fun box(): String {
    if (commonFun() != "1: Int; s: Any") return "FAIL 1"
    if (platformFun() != "1: Int; s: Any") return "FAIL 2"
    if (p != "OK") return "FAIL 3" // Expect property is also should be filtered out on the current site
    return "OK"
}
