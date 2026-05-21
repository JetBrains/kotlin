// LANGUAGE: +MultiPlatformProjects
// callable: sample/some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect fun some(): Int

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

<expr>actual fun some(): Int = 42</expr>
