// ISSUE: KT-86402

// MODULE: LIBRARY
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
class Foo

// FILE: Bar.kt
// LIBRARY_RESOURCE
// A Kotlin source file erroneously bundled inside the binary library's classes root. It must not be visible on the JVM platform.
class Bar

// MODULE: MAIN(LIBRARY)
// FILE: main.kt
class main
