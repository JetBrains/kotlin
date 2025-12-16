// LANGUAGE: +MultiPlatformProjects +ExpectActualClasses
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

expect class AA(x: Int)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

actual class AA
/**
 * [A<caret_1>A]
 * @constructor [A<caret_2>A]
 */
actual constructor(x: Int)
