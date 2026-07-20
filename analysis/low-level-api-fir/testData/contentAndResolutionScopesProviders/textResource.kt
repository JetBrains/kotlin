// ISSUE: KT-86402

// MODULE: LIBRARY
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
class Foo

// FILE: notes.txt
// SKIP_FILE_NAME_VALIDATION
// LIBRARY_RESOURCE
// A non-source text resource. It is not a binary file, so it must not be visible on the JVM platform.
Just some notes.

// MODULE: MAIN(LIBRARY)
// FILE: main.kt
class main
