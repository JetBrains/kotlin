// common.pack.Bar
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
package common.pack

expect class Bar(p1: Int = 0, p2: Int = 0)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// MAIN_MODULE
// FILE: jvm.kt
package common.pack

// All primary constructor parameters have (inherited) default values, so the compiler synthesizes a
// no-arg constructor Bar() on the `actual` class even without @JvmOverloads. The light class must
// mirror that, despite the `actual` parameters having no *declared* defaults.
actual class Bar actual constructor(p1: Int, p2: Int)
