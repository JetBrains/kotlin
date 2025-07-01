// DUMP_CODE

// MODULE: context
// FILE: context.kt

fun main() {
    <caret_stack_1>foo1 {
        fun localFun() {
            return
        }

        class X {
            fun localClassMemberFun() {
                return
            }
        }
    }
}

inline fun foo1(noinline block: () -> Unit) {
    <caret_stack_0>foo2 {
        block();
        repeat(listOf(1).size) {
            return@repeat
        }
        return@foo2 42
    }
}

inline fun foo2(noinline block: () -> Int) {
    <caret_context>val x = 1
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
block()