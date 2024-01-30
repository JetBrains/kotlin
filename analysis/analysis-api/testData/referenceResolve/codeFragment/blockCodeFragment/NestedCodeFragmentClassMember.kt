// IGNORE_FE10

// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}

fun foo() {}


// MODULE: contextFragment
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: contextFragment.kt
// CODE_FRAGMENT_KIND: BLOCK
class Local {
    fun local() {
        Unit
    }
}
<caret_context>Local().local()

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: contextFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
<caret>Local().local()