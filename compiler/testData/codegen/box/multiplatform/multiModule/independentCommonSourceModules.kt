// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common1
// TARGET_PLATFORM: Common
// FILE: common1.kt

fun o() = "O"

// MODULE: common2
// TARGET_PLATFORM: Common
// FILE: common2.kt

fun k() = "K"

// MODULE: jvm()()(common1, common2)
// TARGET_PLATFORM: JVM
// FILE: main.kt

fun box() = o() + k()