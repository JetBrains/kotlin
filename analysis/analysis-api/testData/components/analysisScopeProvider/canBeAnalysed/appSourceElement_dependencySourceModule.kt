// MODULE: dependency
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: dependency.kt
fun foo() { }

// MODULE: app(dependency)
// MODULE_KIND: Source
// FILE: app.kt
fun b<caret>ar() { }
