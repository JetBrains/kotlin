// A non-JVM library, so that its declarations get symbol light classes instead of ones built over decompiled Java stubs.
// The main module refines the library, so light classes for the library declarations are created in the context of the main module,
// where the library declarations are deserialized instead of being analyzed as decompiled sources.

// MODULE: lib
// TARGET_PLATFORM: JS
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

object LibraryObject

class LibraryClass(val value: Int)

// MODULE: main()()(lib)
// TARGET_PLATFORM: JVM
// FILE: main.kt
class ClassWithDefaultParameterValues(val value: Int = 0)

object SourceObject
