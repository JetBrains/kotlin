// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>Unit
}

fun foo() {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: main.kt
// CODE_FRAGMENT_KIND: EXPRESSION
object {
    fun foo() {}

    fun bar() {
        foo()
    }
}