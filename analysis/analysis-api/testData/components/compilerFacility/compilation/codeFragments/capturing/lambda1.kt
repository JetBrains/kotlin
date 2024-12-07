// MODULE: context
// ISSUE: KT-68701

// FILE: context.kt

private val privateLambda: () -> String = { "hello" }

fun test() {
    <caret_context>foo()
}

fun foo() {}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
privateLambda()
