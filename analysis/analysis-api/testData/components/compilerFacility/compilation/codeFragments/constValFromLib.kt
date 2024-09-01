// MODULE: context

// FILE: context.kt
package lib

private const val a: Int = <caret_context>1 shl (Int.SIZE_BITS - 2)


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
lib.a
