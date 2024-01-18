// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    fun local() {}
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<caret>local()