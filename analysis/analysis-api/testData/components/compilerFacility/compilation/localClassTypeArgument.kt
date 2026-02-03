// MODULE: context
// FILE: source.kt
package test

fun test() {
    class Label(val a: String)
    val res = listOf(Label("abc"))
    <caret_context>
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
res