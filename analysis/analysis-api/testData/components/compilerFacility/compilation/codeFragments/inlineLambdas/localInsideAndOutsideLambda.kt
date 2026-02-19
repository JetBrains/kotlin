// DUMP_CODE

// MODULE: context
// FILE: context.kt

fun main() {
    fun localOutsideLambda(x: Int) = x + 1
    <caret_stack_0>foo {
        class X {
            fun memberInLocalClass(x: Int) = x + 2
        }
        fun localInsideLambda(x : Int) = x + 3
        localInsideLambda(it) + localOutsideLambda(it) + X().memberInLocalClass(it)
    }
}

inline fun foo(block: (Int) -> Int) {
    <caret_context>val x = 1
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
block(1)