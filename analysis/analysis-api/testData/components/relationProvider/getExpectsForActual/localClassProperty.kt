// LANGUAGE: +MultiPlatformProjects

// Declarations inside local classes are not addressable by 'TestSymbolTarget'
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Foo {
    val name: String
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

fun outer() {
    actual class Foo {
        <expr>actual val name: String
            get() = "JVM"</expr>
    }
}
