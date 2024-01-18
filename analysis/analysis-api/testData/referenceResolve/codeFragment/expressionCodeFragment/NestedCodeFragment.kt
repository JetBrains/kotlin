// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}

fun foo() {}
fun bar() {}


// MODULE: contextFragment
// MODULE_KIND: CodeFragment

// FILE: contextFragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<caret_context>foo()

// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<caret>bar()