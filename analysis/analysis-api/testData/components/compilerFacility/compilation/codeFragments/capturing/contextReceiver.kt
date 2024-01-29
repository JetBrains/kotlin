// LANGUAGE: +ContextReceivers

// MODULE: context

// FILE: context.kt
fun test() {
    with(Foo()) {
        call()
    }
}

context(Foo)
fun call() {
    <caret_context>val x = 0
}

class Foo {
    val foo: String = "foo"
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo