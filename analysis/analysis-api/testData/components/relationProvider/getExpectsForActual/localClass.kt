// LANGUAGE: +MultiPlatformProjects

// Local classes are not addressable by 'TestSymbolTarget'
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Foo

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// The 'actual' modifier is not applicable to a local class
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

fun outer() {
    <expr>actual class Foo</expr>
}
