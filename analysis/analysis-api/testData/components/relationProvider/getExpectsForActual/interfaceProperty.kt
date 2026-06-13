// LANGUAGE: +MultiPlatformProjects
// callable: sample/Box.value

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect interface Box {
    val value: String
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual interface Box {
    <expr>actual val value: String</expr>
}
