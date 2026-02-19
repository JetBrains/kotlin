// MODULE: context
// ISSUE: KT-70861

// FILE: context.kt

private class Clazz

fun test() {
    val a = Clazz::class
    <caret_context>foo()
}

fun foo() {}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Clazz::class
