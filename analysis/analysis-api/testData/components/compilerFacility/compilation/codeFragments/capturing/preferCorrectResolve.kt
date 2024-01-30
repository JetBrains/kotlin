// MODULE: context

// FILE: context.kt
class A {
    private fun foo(x: Int) = 1 + x
    fun foo(x: Any) = 2
}

fun test(a: A) {
    <caret_context>val x = 0
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
// Need to choose public foo(Any) call instead of more specific but private call foo(Int)
a.foo(5)