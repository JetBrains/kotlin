// LANGUAGE: +MultiPlatformProjects

// Local functions are not addressable by 'TestSymbolTarget'
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun some(): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// The 'actual' modifier is not applicable to a local function
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

fun outer() {
    <expr>actual fun some(): Int = 42</expr>
}
