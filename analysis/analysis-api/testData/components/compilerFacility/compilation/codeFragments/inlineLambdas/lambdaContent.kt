// DUMP_CODE

// MODULE: context
// FILE: context.kt

fun main() {
    <caret_stack_0>foo { it ->

        fun localFunWithLocalClass(p: Int): Int {
            class X(val x: Int)
            return X(p).x
        }

        fun createFunWithLocalObj(): () -> Int {
            return {
                object {
                    val x = 1
                }.x
            }
        }

        val complexLambda = object {
            val prop = {

                var x = 10
                x += 100

                val y = localFunWithLocalClass(1000)

                val z = createFunWithLocalObj()()

                val l = { 10000 }

                x + y + z + l()
            }
        }.prop

        complexLambda() + it
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
block(100000)