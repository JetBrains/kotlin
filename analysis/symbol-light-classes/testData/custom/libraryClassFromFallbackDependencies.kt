// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

class LibraryClass(val value: Int)

// MODULE: main
// MODULE_KIND: LibrarySource
// FALLBACK_DEPENDENCIES
// FILE: main.kt
fun usage(value: lib.LibraryClass) {}
