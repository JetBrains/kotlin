// common.pack.Derived
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
package common.pack

expect open class Base {
    @JvmOverloads
    open fun foo(p1: Int = 0, p2: Int = 0, p3: Int = 0): Unit
}

expect class Derived : Base {
    @Suppress("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS")
    @JvmOverloads
    override fun foo(p1: Int, p2: Int, p3: Int): Unit
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// MAIN_MODULE
// FILE: jvm.kt
package common.pack

actual open class Base {
    @JvmOverloads
    actual open fun foo(p1: Int, p2: Int, p3: Int) = Unit
}

// The default values are declared on the `expect` `Base.foo`. `Derived.foo` merely overrides it, so - just like in
// the non-multiplatform case - `@JvmOverloads` has no effect on the override: the overloads belong to `Base`, and the
// compiler reports OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS. `Derived` must therefore expose only the full `foo(Int, Int, Int)`.
actual class Derived : Base() {
    @Suppress("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS")
    @JvmOverloads
    actual override fun foo(p1: Int, p2: Int, p3: Int) = Unit
}
