// ISSUE: KT-86402

// MODULE: LIBRARY
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
class Foo

// FILE: Script.kts
// LIBRARY_RESOURCE
// A Kotlin script erroneously bundled inside the binary library's classes root. It must not be visible on the JVM platform.
val script = 1

// MODULE: MAIN(LIBRARY)
// FILE: main.kt
class main
