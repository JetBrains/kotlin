// common.pack.Foo
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
package common.pack

expect class Foo {
    @JvmOverloads
    fun foo(p1: Int = 0, p2: Int = 0, p3: Int = 0): Unit
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// MAIN_MODULE
// FILE: jvm.kt
package common.pack

actual class Foo {
    // The default parameter values are inherited from the `expect` declaration and
    // cannot be repeated here, so the `actual` value parameters have no *declared* defaults.
    @JvmOverloads
    actual fun foo(p1: Int, p2: Int, p3: Int) = Unit
}
