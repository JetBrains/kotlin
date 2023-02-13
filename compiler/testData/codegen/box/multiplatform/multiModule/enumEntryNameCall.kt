// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

enum class Base1 { O }

expect enum class Base2 { K }

fun k() = Base2.K.name

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual enum class Base2 { K }

fun box() = Base1.O.name + k()