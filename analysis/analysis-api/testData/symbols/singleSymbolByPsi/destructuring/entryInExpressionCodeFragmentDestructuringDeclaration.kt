// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// MODULE: context

// FILE: context.kt
data class X(val a: Int, val b: Int)

fun test(x: X) {
    <caret_context>Unit
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
@Suppress("UNUSED_VARIABLE") val (<caret>c, d) = x
