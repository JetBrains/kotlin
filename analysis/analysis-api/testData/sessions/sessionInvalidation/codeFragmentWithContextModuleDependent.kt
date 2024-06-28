// MODULE: context
// MODULE_KIND: Source
// FILE: context.kt
fun foo() {
    <caret_context>val x = 0
}

// MODULE: dependent(context)
// MODULE_KIND: Source
// WILDCARD_MODIFICATION_EVENT
// FILE: dependent.kt
fun dependent() {
    val y = 1
}

// MODULE: fragment1.kt
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment1.kt
// CODE_FRAGMENT_KIND: BLOCK
<caret>foo()
