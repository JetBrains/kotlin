// MODULE: context

// FILE: context.kt
class Foo {
    fun test() {
        fun call() {
            consume(this@Foo)
        }

        <caret_context>call()
    }
}

fun consume(obj: Foo) {}


// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call()