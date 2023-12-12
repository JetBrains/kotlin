// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-62926

// MODULE: common
// FILE: common.kt
fun commonBox(): String {
    return "".k + 1.k
}

expect val Int.k: String
expect val String.k: String

// MODULE: platform()()(common)
// FILE: platform.kt
actual val Int.k: String get() = "K"
actual val String.k: String get() = "O"

fun box(): String {
    return commonBox()
}
