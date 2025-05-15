// MODULE: context

// FILE: context.kt
fun main() {
    <caret_context>val a = 1
}

private inline fun foo() = object : Function<String> {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context
foo()