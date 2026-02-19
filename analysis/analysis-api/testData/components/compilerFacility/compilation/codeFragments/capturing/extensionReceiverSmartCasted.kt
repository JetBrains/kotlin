// MODULE: context

// FILE: context.kt
interface Foo

class FooImpl : Foo {
    val n: Int = 5
}

fun makeFoo(): Foo = FooImpl()

fun main() {
    makeFoo().apply {
        <caret_context>Unit
    }
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
this as FooImpl
n