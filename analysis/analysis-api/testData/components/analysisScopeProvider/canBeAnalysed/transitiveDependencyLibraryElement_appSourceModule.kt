// IGNORE_STANDALONE
// KT-76818

// MODULE: dependency1
// MODULE_KIND: LibraryBinary
// FILE: dependency1.kt
fun foo() { }

// MODULE: dependency2(dependency1)
// MODULE_KIND: Source
// FILE: dependency2.kt

// MODULE: app(dependency2)
// MODULE_KIND: Source
// USE_SITE_MODULE
// MAIN_MODULE
// FILE: app.kt
fun bar() { }

// callable: foo
