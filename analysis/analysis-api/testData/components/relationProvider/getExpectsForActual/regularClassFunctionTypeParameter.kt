// LANGUAGE: +MultiPlatformProjects
// type_parameter: T: callable: sample/Platform.some

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Platform {
    fun <T : Any> some(): T
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Platform {
    actual fun <<expr>T : Any</expr>> some(): T = null!!
}
