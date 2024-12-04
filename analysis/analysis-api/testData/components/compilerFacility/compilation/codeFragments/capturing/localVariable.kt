// MODULE: context

// FILE: context.kt
fun test() {
    var x = "a"
    <caret_context>x
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
x = "O"
x + "K"