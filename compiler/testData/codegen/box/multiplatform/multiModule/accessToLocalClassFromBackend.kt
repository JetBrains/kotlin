// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

val LocalClass = object {
    override fun toString() = "OK"
}

fun ok() = LocalClass.toString()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

fun box() = ok()