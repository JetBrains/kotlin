// ISSUE: KT-86402

// MODULE: LIBRARY
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
class Foo

// FILE: LICENSE
// SKIP_FILE_NAME_VALIDATION
// LIBRARY_RESOURCE
// A file without an extension. It must not be visible on the JVM platform.
All rights reserved.

// MODULE: MAIN(LIBRARY)
// FILE: main.kt
class main
