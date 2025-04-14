// MODULE: main
// MODULE_KIND: Source
// USE_SITE_MODULE
// FILE: main.kt
val a: Int = 0
val b: Int = 5
val c: Int = 10

// MODULE: fragment1.kt
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: main
// FILE: fragment1.kt
// CODE_FRAGMENT_KIND: BLOCK
fun b<caret>ar(): Int {
    return a + b
}
