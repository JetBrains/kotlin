// MODULE: context

// FILE: context.kt
object Foo {
    fun test() {
        <caret_context>val x = 0
    }

    fun call() {}
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
call()