// DUMP_CODE

// MODULE: context
// FILE: context.kt

fun main() {
    val localVal = 1
    val localLazyVal by lazy { 2 }
    var localVar = 3

    val test = X(4)

    test.<caret_stack_3>foo1(localVal + 1) {
        localVar *= 5
        localVar + localLazyVal
    }
}

class X(val x: Int) {

    val propLazy by lazy { 6 }

    var propVar: Int = 7
        set(value) {
            foo4 {
                field + 8
            }
            field = value
        }

    inline fun foo1(p: Int, block: () -> Int) {
        val localVal = 9
        <caret_stack_2>foo2 { p + localVal + block() + propLazy + x }
    }

    inline fun foo2(block: () -> Int) {
        var localVar = 10
        val localLazyVal by lazy { 11 }
        <caret_stack_1>foo3 {
            localVar += 12
            block() + localVar + localLazyVal
        }
    }

    inline fun foo3(block: () -> Int) {
        <caret_stack_0>foo4 { block() + propVar }
    }
}

inline fun foo4(block: () -> Int) {
    <caret_context>val x = 1
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
block()