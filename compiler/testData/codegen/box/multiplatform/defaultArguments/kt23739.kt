// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: ANY
// FIR status: outdated code (expect and actual in the same module)

// FILE: common.kt

// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES
// A LOT OF LINES

expect inline fun <T> get(p: String = "OK"): String

// FILE: platform.kt

actual inline fun <T> get(p: String): String {
    return p
}

fun box(): String {
    return get<String>()
}
