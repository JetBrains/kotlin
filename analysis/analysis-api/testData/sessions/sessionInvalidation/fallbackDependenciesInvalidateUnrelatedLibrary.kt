// MODULE: unrelatedLibrary
// MODULE_KIND: LibraryBinary
// WILDCARD_MODIFICATION_EVENT
// FILE: unrelatedLibrary.kt
class UnrelatedLibrary

// MODULE: unrelatedSourceModule
// MODULE_KIND: Source
// FILE: unrelatedSourceModule.kt
class UnrelatedSourceModule

// MODULE: library1
// MODULE_KIND: LibraryBinary
// FALLBACK_DEPENDENCIES
// FILE: C1.kt
class C1

// MODULE: library2
// MODULE_KIND: LibraryBinaryDecompiled
// FALLBACK_DEPENDENCIES
// FILE: C2.kt
class C2

// MODULE: librarySource
// MODULE_KIND: LibrarySource
// FALLBACK_DEPENDENCIES
// FILE: librarySource.kt
class LibrarySource

// MODULE: sourceModule(library1, library2)
// MODULE_KIND: Source
// FILE: sourceModule.kt
class SourceModule
