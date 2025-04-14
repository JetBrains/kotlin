// IGNORE_STANDALONE
// KT-76042

// MODULE: unrelated
// MODULE_KIND: LibraryBinary
// USE_SITE_MODULE
// FILE: unrelated.kt
fun foo() { }

// MODULE: app
// MODULE_KIND: Source
// FILE: app.kt
fun b<caret>ar() { }
