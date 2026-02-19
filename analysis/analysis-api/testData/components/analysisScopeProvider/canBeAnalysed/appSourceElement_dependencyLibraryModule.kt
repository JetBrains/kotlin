// IGNORE_STANDALONE
// KT-76042

// MODULE: library
// MODULE_KIND: LibraryBinary
// USE_SITE_MODULE
// FILE: library.kt
fun foo() { }

// MODULE: app(library)
// MODULE_KIND: Source
// FILE: app.kt
fun b<caret>ar() { }
