// common.pack.Foo
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
package common.pack

expect class Foo @JvmOverloads constructor(p1: Int = 0, p2: Int = 0, p3: Int = 0)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// MAIN_MODULE
// FILE: jvm.kt
package common.pack

// The default parameter values are inherited from the `expect` constructor, so the `actual`
// constructor parameters have no *declared* defaults. The @JvmOverloads variants
// Foo(), Foo(int) and Foo(int, int) must still be generated on the `actual` class.
actual class Foo @JvmOverloads actual constructor(p1: Int, p2: Int, p3: Int)
