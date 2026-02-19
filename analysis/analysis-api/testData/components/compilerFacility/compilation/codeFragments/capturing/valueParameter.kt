// MODULE: context

// FILE: context.kt
fun test(foo: Foo?) {
    <caret_context>val x = 0
}

class Foo {
    fun call() {}
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo?.call()