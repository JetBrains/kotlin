// MODULE: context

// FILE: dep.kt
inline fun foo(): String {
    return "OK"
}

inline fun bar() {
    baz()
}

// FILE: context.kt
inline fun baz() {
    foo()
}

fun main() {
    <caret_context>Unit
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo()