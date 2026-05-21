// LANGUAGE: +MultiPlatformProjects

// Constructors are not addressable by 'TestSymbolTarget'
// DISABLE_COORDINATE_TEST

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Foo(n: Int)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Foo <expr>actual constructor(n: Int)</expr>
