// MODULE: LIBRARY
// MODULE_KIND: LibraryBinary
// FILE: a.kt
class a

// FILE: b.kt
// SHADOWED
class b

// MODULE: LIBRARY_REFINER
// MODULE_KIND: LibraryBinary
// FILE: x.kt
// ADDED
class x

// MODULE: MAIN(LIBRARY)
// MODULE_KIND: LibrarySource
// FILE: main.kt
class main
