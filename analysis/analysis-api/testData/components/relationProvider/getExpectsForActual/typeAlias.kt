// LANGUAGE: +MultiPlatformProjects
// typealias: sample/Platform

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

class PlatformImpl

<expr>actual typealias Platform = PlatformImpl</expr>
