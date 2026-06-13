// LANGUAGE: +MultiPlatformProjects
// callable: sample/size

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect val String.size: Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual val String.size: Int
    get() = length</expr>
