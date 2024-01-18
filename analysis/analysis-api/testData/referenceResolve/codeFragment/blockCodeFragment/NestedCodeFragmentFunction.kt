// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}

fun foo() {}


// MODULE: contextFragment
// MODULE_KIND: CodeFragment

// FILE: contextFragment.kt
// CODE_FRAGMENT_KIND: BLOCK
fun bar() {}
<caret_context>bar()

// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
<caret>bar()