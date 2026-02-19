// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt

package test

expect val a: String.(String) -> String


// MODULE: platform()()(common)
// FILE: platform.kt

package test

actual val a = fun String.(y: String): String { return this + y }

fun box(): String {
    return if (a("O", "K") == "O".a("K")) "OK" else "FAIL"
}


