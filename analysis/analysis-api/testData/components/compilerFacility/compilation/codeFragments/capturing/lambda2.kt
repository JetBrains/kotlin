// MODULE: context
// ISSUE: KT-70390

// FILE: context.kt

fun test() {
    <caret_context>foo(Clazz().privateClazzLambda())
}

fun foo(s: String) {}

private class Clazz {
    val privateClazzLambda: () -> String = { "privateClazzLambda" }
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Clazz().privateClazzLambda()

