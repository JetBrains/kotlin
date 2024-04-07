// MODULE: context

// FILE: context.kt
fun test() {
    <caret_context>call("foo")
}

fun call(text: String) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
fun local() {
    call("bar")
}

<caret>local()