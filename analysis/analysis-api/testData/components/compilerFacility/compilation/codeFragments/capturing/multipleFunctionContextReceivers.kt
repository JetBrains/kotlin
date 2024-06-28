// LANGUAGE: +ContextReceivers

// MODULE: context

// FILE: context.kt
class Ctx1
class Ctx2

context(Ctx1, Ctx2)
fun useWithCtx1Ctx2() = 3

context(Ctx1, Ctx2)
fun foo() {
    <caret_context>val x = 1
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
useWithCtx1Ctx2()