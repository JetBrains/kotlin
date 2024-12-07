// MODULE: context

// FILE: context.kt
class A {
    private fun foo() = 2
}

fun test(a: A) {
    <caret_context>val x = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
a.foo()