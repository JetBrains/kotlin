// MODULE: dependency
// MODULE_KIND: Source
// FILE: dependency.kt
fun f<caret>oo() { }

// MODULE: app(dependency)
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: app.kt
fun bar() { }
