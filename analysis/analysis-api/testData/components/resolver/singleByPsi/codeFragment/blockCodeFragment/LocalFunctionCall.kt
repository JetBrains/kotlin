// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    fun local() {}
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
<caret>local()