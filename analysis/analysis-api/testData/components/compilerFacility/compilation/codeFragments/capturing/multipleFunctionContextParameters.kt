// LANGUAGE: +ContextParameters

// MODULE: context

// FILE: context.kt
class Ctx1
class Ctx2

context(_: Ctx1, _: Ctx2)
fun useWithCtx1Ctx2() = 3

context(_: Ctx1, _: Ctx2)
fun foo() {
    <caret_context>val x = 1
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
useWithCtx1Ctx2()