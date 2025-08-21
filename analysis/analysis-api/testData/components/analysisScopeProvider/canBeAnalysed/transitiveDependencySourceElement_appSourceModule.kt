// MODULE: dependency1
// MODULE_KIND: Source
// FILE: dependency1.kt
fun f<caret>oo() { }

// MODULE: dependency2(dependency1)
// MODULE_KIND: Source
// FILE: dependency2.kt

// MODULE: app(dependency2)
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: app.kt
fun bar() { }
