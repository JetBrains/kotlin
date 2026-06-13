// LANGUAGE: +MultiPlatformProjects
// constructor: sample/Foo.init(n)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Foo(n: Int)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// The 'actual' constructor is intentionally missing the 'actual' modifier
// COMPILATION_ERRORS

// FILE: Jvm.kt

package sample

actual class Foo<expr>(n: Int)</expr>
