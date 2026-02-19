// MODULE: context
// MODULE_KIND: Source
// FILE: main.kt
private fun foo(p: Int) {}

fun test() {
    println(<caret_context>)
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context
// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
// CODE_FRAGMENT_RESOLUTION_MODE: IGNORE_SELF
println(<caret>)

// callable: /foo
