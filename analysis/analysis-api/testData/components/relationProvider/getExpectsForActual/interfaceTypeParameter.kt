// LANGUAGE: +MultiPlatformProjects
// type_parameter: T: class: sample/Box

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect interface Box<T> {
    val value: T
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual interface Box<<expr>T</expr>> {
    actual val value: T
}
