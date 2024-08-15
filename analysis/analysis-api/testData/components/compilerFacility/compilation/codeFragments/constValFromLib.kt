// Currently fails with:
//   Error occurred while optimizing an expression:
//   CALL 'private final fun <get-a> (): kotlin.Int declared in lib.ContextKt' type=kotlin.Int origin=GET_PROPERTY

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
