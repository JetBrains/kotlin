// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class Foo

expect fun getFoo(): Foo

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

actual open class Foo

class Bar : Foo()

actual fun getFoo(): Foo = Bar()
