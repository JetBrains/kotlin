// MODULE: context

// FILE: context.kt
class Outer {
    class Inner {
        fun test() {
            fun call(): Int {
                consume(this@Inner)
                return 1
            }

            fun call(a: Int): Int {
                consume(this@Inner)
                return 1 + a
            }

            fun call(a: Int, f: (Int)->Int): Int {
                consume(this@Inner)
                return 1 + a + f(a)
            }

            fun call2(): Int {
                fun call() = 1
                consume(this@Inner)
                return 2
            }

            fun call3(): Int {
                fun call() = 1
                fun call2() = 2
                consume(this@Inner)
                return 3
            }

            <caret_context>call() + call(4) + call(9) { 2 * it } + call2() + call3()
        }

        fun call() = 1

        fun call2(): Int {
            fun call() = 1
            return 2
        }

        fun call3(): Int {
            fun call() = 1
            fun call2() = 2
            return 3
        }
    }

    fun call() = 1

    fun call2(): Int {
        fun call() = 1
        return 2
    }

    fun call3(): Int {
        fun call() = 1
        fun call2() = 2
        return 3
    }
}

fun consume(obj: Outer.Inner) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call() + call(4) + call(9) { 2 * it } + call2() + call3()