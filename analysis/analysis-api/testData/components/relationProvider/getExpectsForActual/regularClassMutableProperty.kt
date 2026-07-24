// LANGUAGE: +MultiPlatformProjects
// callable: sample/Platform.name

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    var name: String
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    <expr>actual var name: String = "JVM"</expr>
}
