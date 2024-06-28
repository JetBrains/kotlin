// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-60523

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun warn(): String
expect fun error(): String
expect fun hidden(): String

fun common(): String {
    // no deprecation warning
    if (warn() != "OK") return "Warn fail"
    // no deprecation error
    if (error() != "OK") return "Error fail"
    // no compilation error
    if (hidden() != "OK") return "Hidden fail"
    return "OK"
}


// MODULE: lib()()(common)
// FILE: lib.kt

@Deprecated("", level = DeprecationLevel.WARNING)
actual fun warn(): String = "OK"

@Deprecated("", level = DeprecationLevel.ERROR)
actual fun error(): String = "OK"

@Deprecated("", level = DeprecationLevel.HIDDEN)
actual fun hidden(): String = "OK"


// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return common()
}
