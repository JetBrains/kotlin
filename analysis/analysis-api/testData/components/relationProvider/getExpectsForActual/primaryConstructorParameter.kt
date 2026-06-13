// LANGUAGE: +MultiPlatformProjects
// value_parameter: n: constructor: sample/Foo.init(n)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Foo(n: Int)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Foo actual constructor(<expr>n: Int</expr>)
