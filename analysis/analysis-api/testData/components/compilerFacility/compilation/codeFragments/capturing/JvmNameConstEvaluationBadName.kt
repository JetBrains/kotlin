// MODULE: context
// COMPILATION_ERRORS

// FILE: context.kt
const val prefix = "prefix_"

@JvmName(boo = "${prefix}f1")
fun f1() = 42

fun main() {
    <caret_context>val x = 1
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
f1()