// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: default argument mapping in MPP isn't designed yet
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
