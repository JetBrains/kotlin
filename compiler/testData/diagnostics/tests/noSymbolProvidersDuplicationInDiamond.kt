// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: common
expect fun g0(): String

// MODULE: intermediate1()()(common)

// MODULE: intermediate2()()(common)

// MODULE: main()()(intermediate1, intermediate2)

actual fun g0(): String = "OK"
