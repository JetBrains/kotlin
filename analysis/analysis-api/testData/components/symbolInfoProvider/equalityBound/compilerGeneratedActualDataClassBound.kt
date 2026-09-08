// LANGUAGE: +StrictEquals +MultiPlatformProjects
// function: /Data.equals(other)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
expect class Data {
    override fun equals(@EqualityBound(Data::class) other: Any?): Boolean
}

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: platform.kt
actual data class Data(val value: Int)
