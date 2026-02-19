// MODULE: context

//FILE: context.kt
fun main() {
    class Foo
    <caret_context>output("hi")
}

fun output(text: String) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Foo()