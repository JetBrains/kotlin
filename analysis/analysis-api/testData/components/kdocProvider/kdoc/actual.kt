// LANGUAGE: +MultiPlatformProjects
// IGNORE_FE10


// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Common.kt

expect class Foo expect constructor() {
    fun documented(a: String)
}


// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt

actual class Foo actual constructor() {
    /**
     * Actual KDoc
     *
     * @param a in actual
     */
    actual fun documented(a: String) {}
}

// MODULE: main(jvm)
// FILE: main.kt
fun main() {
    val f = Foo()
    f.documented(a = "string")
}