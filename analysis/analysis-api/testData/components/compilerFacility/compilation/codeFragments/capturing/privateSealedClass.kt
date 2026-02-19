// MODULE: context
// ISSUE: KT-70893

// FILE: context.kt

fun test() {
    val c = Expr.Const()
    val na = Expr.NotANumber
    <caret_context>foo()
}

fun foo() {}

private sealed class Expr {
    class Const() : Expr()
    object NotANumber : Expr()
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Expr.Const()
