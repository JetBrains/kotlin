// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt
package test

fun test(foo: Foo) {
    <caret>foo.call()
}

expect class Foo {
    fun call()
}

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt
package test

actual class Foo {
    fun call() {}
}