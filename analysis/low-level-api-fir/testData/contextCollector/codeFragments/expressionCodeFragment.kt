// MODULE: context

// FILE: context.kt
package example

class Foo {
    val n = 42
    val text = "Hello"
}

fun test() = withFoo {
    <caret_context>text
}

fun withFoo(block: Foo.() -> Unit) {
    val foo = Foo()
    foo.block()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<expr>text + n</expr>
