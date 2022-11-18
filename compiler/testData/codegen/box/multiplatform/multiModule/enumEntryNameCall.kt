// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

enum class Base { OK }

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

fun box() = Base.OK.name