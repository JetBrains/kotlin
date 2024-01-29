// MODULE: context

// FILE: context.kt
package test

class Foo

fun test() {
    val foo = Foo()
    <caret_context>consume(foo)
}

fun consume(foo: Foo) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<expr>foo</expr>