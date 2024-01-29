// MODULE: context

// FILE: context.kt
class Foo {
    fun String.test() {
        var x: Int

        fun call(a: Int) {
            consume(a)
            consume(this@Foo)
            consume(this@test)
            x = 42
        }

        <caret_context>call(1)
    }
}

fun consume(obj: Any) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call(0)