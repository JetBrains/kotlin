// ISSUE: KT-64686
// LANGUAGE: +MultiPlatformProjects
// TARGET_PLATFORM: Common
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package one

expect object Foo {
    val x: Int

    fun foo()
}

// MODULE: main(lib)
// FILE: main.kt

// class: one/Foo
