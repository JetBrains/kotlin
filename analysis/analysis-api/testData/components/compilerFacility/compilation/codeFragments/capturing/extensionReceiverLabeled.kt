// MODULE: context

// FILE: context.kt
fun test() {
    with("Hello, world!") str@{
        <caret_context>val x = 0
    }
}


// MODULE: main
// MODULE_KIND: CodeFragment

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
this@str.length