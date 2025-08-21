// IGNORE_STANDALONE
// KT-76042

// MODULE: dependency1
// MODULE_KIND: LibraryBinary
// USE_SITE_MODULE
// FILE: dependency1.kt
fun foo() { }

// MODULE: dependency2(dependency1)
// MODULE_KIND: Source
// FILE: dependency2.kt

// MODULE: app(dependency2)
// MODULE_KIND: Source
// FILE: app.kt
fun b<caret>ar() { }
