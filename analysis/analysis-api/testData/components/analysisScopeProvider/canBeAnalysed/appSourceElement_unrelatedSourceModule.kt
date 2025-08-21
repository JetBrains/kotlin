// MODULE: unrelated
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: unrelated.kt
fun foo() { }

// MODULE: app
// MODULE_KIND: Source
// FILE: app.kt
fun b<caret>ar() { }
