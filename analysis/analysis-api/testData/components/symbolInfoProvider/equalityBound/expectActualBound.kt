// LANGUAGE: +StrictEquals +MultiPlatformProjects
// function: /Platform.equals(other)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
expect class Platform {
    override fun equals(@EqualityBound(Platform::class) other: Any?): Boolean
}

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: platform.kt
actual class Platform {
    actual override fun equals(@EqualityBound(Platform::class) other: Any?): Boolean = true
}
