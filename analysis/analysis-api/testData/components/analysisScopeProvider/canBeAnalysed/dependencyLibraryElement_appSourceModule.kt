// IGNORE_STANDALONE
// KT-76818

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: library.kt
fun foo() { }

// MODULE: app(library)
// MODULE_KIND: Source
// USE_SITE_MODULE
// MAIN_MODULE
// FILE: app.kt
fun bar() { }

// callable: foo
