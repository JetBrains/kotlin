// MODULE: source_module
// MODULE_KIND: Source
// FILE: source.kt
private fun foo(p: Int) {}

fun test() {
    println(<caret_context>)
}

// MODULE: intermediate
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: source_module
// FILE: fragment_intermediate.kt
// CODE_FRAGMENT_KIND: EXPRESSION
// CODE_FRAGMENT_RESOLUTION_MODE: IGNORE_SELF
println(<caret_context>)

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: intermediate
// FILE: fragment_main.kt
// CODE_FRAGMENT_KIND: EXPRESSION
// CODE_FRAGMENT_RESOLUTION_MODE: IGNORE_SELF
println(<caret>)

// callable: /foo
