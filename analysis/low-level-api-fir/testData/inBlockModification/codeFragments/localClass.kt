// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
class Foo(val x: Int) {
    fun foo() {
        <expr>foo()</expr>
    }
}

Foo(1)