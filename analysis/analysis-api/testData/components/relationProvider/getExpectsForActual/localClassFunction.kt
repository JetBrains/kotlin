// LANGUAGE: +MultiPlatformProjects

// Declarations inside a local class are not addressable by 'TestSymbolTarget'
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Foo {
    fun bar(): Int
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// The 'actual' modifier is not applicable to a local class
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

fun outer() {
    actual class Foo {
        <expr>actual fun bar(): Int = 42</expr>
    }
}
