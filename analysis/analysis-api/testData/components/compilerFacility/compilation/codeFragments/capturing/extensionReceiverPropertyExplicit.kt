// MODULE: context

// FILE: context.kt
fun test() {
    1.ext
}

val Int.ext: String
    get() {
        <caret_context>return "foo"
    }


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
this.dec()