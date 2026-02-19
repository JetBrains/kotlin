// LANGUAGE: +MultiPlatformProjects
// IGNORE_FE10

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Common.kt
expect class Foo expect constructor() {
    /**
     * Expect KDoc
     */
    fun documented()

    /**
     * Expect KDoc
     */
    fun undocumented() {}
}


// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt
actual class Foo actual constructor() {
    /**
     * Actual KDoc
     */
    actual fun documented() {}

    actual fun undocumented() {}
}

// MODULE: main(jvm)
// FILE: main.kt
fun main() {
    val f = Foo()
    f.documented()
    f.undocumented()
}