// MODULE: context

// FILE: context.kt
class Foo(a: Int, b: String) {
    init {
        <caret_context>val x = 0
    }
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
a + b.length