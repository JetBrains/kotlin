// FIR_IDENTICAL
// !LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: common
// TARGET_PLATFORM: Common
expect fun g0(): String

// MODULE: intermediate1()()(common)
// TARGET_PLATFORM: Common

// MODULE: intermediate2()()(common)
// TARGET_PLATFORM: Common

// MODULE: main()()(intermediate1, intermediate2)
// TARGET_PLATFORM: JVM

actual fun g0(): String = "OK"
