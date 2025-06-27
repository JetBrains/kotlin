// WITH_STDLIB

// MODULE: context

//FILE: context.kt
fun test() {
    class Local
    val lc = Local()
    lc.toString() // Explicit substitution override usage in the context file
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
lc.toString()