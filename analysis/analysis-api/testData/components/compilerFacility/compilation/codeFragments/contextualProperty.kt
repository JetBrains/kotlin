// LANGUAGE: +ContextParameters
// MODULE: context

// FILE: context.kt
context(_: String)
val hello: String
    get() = "hello"

fun String.main() {
    hello<caret_context>
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
hello