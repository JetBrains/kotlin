// ISSUE: KT-64686
// LANGUAGE: +MultiPlatformProjects
// TARGET_PLATFORM: Common
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package one

expect class Foo {
    companion object {
        val x: Int

        fun foo()
    }
}

// MODULE: main(lib)
// FILE: main.kt

// class: one/Foo.Companion
